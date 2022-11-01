package part2_lowlevelserver

import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, PostStop}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.{IncomingConnection, ServerBinding}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Sink
import akka.util.Timeout
import spray.json._
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success}

case class Guitar(make: String, model: String, quantity: Int)

object GuitarDB {
  trait Command
  case class CreateGuitar(guitar: Guitar, replyTo: ActorRef[GuitarCreated]) extends Command
  case class FindGuitar(id: Int, replyTo: ActorRef[Option[Guitar]]) extends Command
  case class FindAllGuitars(replyTo: ActorRef[List[Guitar]]) extends Command
  case class AddQuantity(id: Int, quantity: Int, replyTo: ActorRef[Option[Guitar]]) extends Command
  case class FindGuitarsByStock(inStock: Boolean, replyTo: ActorRef[List[Guitar]]) extends Command

  trait Event
  case class GuitarCreated(id: Int) extends Event

  def apply(): Behavior[Command] = state(0, Map())

  def state(currentId: Int, guitars: Map[Int, Guitar]): Behavior[Command] = Behaviors.receive { (ctx, msg) =>
    msg match {
      case FindAllGuitars(replyTo) =>
        ctx.log.info("Searching for all guitars...")
        replyTo ! guitars.values.toList
        Behaviors.same
      case FindGuitar(id, replyTo) =>
        ctx.log.info(s"Searching guitar by id: $id")
        replyTo ! guitars.get(id)
        Behaviors.same
      case FindGuitarsByStock(inStock, replyTo) =>
        ctx.log.info(s"Searching for all guitars in ${if (inStock) "in" else "out of" } stock")
        if (inStock)
          replyTo ! guitars.values.filter(_.quantity > 0).toList
        else
          replyTo ! guitars.values.filter(_.quantity == 0).toList

        Behaviors.same
      case CreateGuitar(guitar, replyTo) =>
        ctx.log.info(s"Adding guitar $guitar with id $currentId")
        replyTo ! GuitarCreated(currentId)
        state(currentId + 1, guitars + (currentId -> guitar))
      case AddQuantity(id, quantity, replyTo) =>
        ctx.log.info(s"Trying to add $quantity items for guitar: $id")
        val guitar = guitars.get(id)
        val newGuitar = guitar.map {
          case Guitar(make, model, q) => Guitar(make, model, q + quantity)
        }
        replyTo ! newGuitar
        newGuitar match {
          case Some(guitar) => state(currentId, guitars + (id -> guitar))
          case None => Behaviors.same
        }
      case _ => Behaviors.same
    }
  }
}

trait GuitarJsonSupport extends SprayJsonSupport {
  implicit val guitarFormat: RootJsonFormat[Guitar] = jsonFormat3(Guitar)
}

object Server extends GuitarJsonSupport {

  sealed trait Message
  private final case class StartFailed(cause: Throwable) extends Message
  private final case class Started(binding: ServerBinding) extends Message
  case object Stop extends Message

  def apply(host: String, port: Int): Behavior[Message] = Behaviors.setup { ctx =>
    import akka.actor.typed.scaladsl.AskPattern.schedulerFromActorSystem
    import part2_lowlevelserver.GuitarDB._

    implicit val system: ActorSystem[Nothing] = ctx.system
    implicit val executionContext: ExecutionContextExecutor = system.executionContext
    implicit val timeout: Timeout = Timeout(2 seconds)

    val guitarsDb = ctx.spawn(GuitarDB(), "GuitarsDB")

    val asyncRequestHandler: HttpRequest => Future[HttpResponse] = {
      case HttpRequest(HttpMethods.POST, uri@Uri.Path("/api/guitar/inventory"), _, _, _) =>
        val query = uri.query() // query object <=> map[String, String]
        val guitarId = query.get("id").map(_.toInt)
        val guitarQty = query.get("qty").map(_.toInt)

        val validGuitarResponseFuture = for {
          id <- guitarId
          qty <- guitarQty
        } yield {
          val newGuitarFuture = guitarsDb.ask(AddQuantity(id, qty, _)).mapTo[Option[Guitar]]
          newGuitarFuture.map(_ => HttpResponse(StatusCodes.OK))
        }

        validGuitarResponseFuture.getOrElse(Future(HttpResponse(StatusCodes.BadRequest)))
      case HttpRequest(HttpMethods.GET, uri@Uri.Path("/api/guitar/inventory"), _, _, _)  =>
        val query = uri.query()
        val inStock = query.get("inStock").map(_.toBoolean)

        val guitarsFuture = guitarsDb.ask(FindGuitarsByStock(inStock.getOrElse(false), _)).mapTo[List[Guitar]]
        guitarsFuture.map { guitars =>
          HttpResponse(
            entity = HttpEntity(
              ContentTypes.`application/json`,
              guitars.toJson.prettyPrint
            )
          )
        }

      case HttpRequest(HttpMethods.GET, uri@Uri.Path("/api/guitar"), _, _, _) =>
        val query = uri.query() // query object <=> map[String, String]
        if (query.isEmpty) {
          val guitarsFuture = (guitarsDb ? FindAllGuitars).mapTo[List[Guitar]]
          guitarsFuture.map { guitars =>
            HttpResponse(
              entity = HttpEntity(
                ContentTypes.`application/json`,
                guitars.toJson.prettyPrint
              )
            )
          }
        } else {
          val guitarId = query.get("id").map(_.toInt)
          guitarId match {
            case None => Future(HttpResponse(StatusCodes.NotFound))
            case Some(id: Int) =>
              val getGuitarFuture = guitarsDb.ask(FindGuitar(id, _)).mapTo[Option[Guitar]]
              getGuitarFuture.map {
                case None => HttpResponse(StatusCodes.NotFound)
                case Some(guitar) => HttpResponse(
                  entity = HttpEntity(
                    ContentTypes.`application/json`,
                    guitar.toJson.prettyPrint
                  )
                )
              }
          }
        }
      case HttpRequest(HttpMethods.POST, Uri.Path("/api/guitar"), _, entity, _) =>
        val strictEntityFuture = entity.toStrict(3 seconds)
        strictEntityFuture.flatMap { strictEntity =>
          val guitarJsonString = strictEntity.data.utf8String
          val guitar = guitarJsonString.parseJson.convertTo[Guitar]
          val guitarCreatedFuture = guitarsDb.ask(CreateGuitar(guitar, _)).mapTo[GuitarCreated]
          guitarCreatedFuture.map { _ =>
            HttpResponse(StatusCodes.OK)
          }
        }
      case request: HttpRequest =>
        request.discardEntityBytes()
        Future(HttpResponse(StatusCodes.NotFound))
    }

    val serverBinding = Http().newServerAt(host, port).bind(asyncRequestHandler)
    ctx.pipeToSelf(serverBinding) {
      case Success(binding) => Started(binding)
      case Failure(ex)      => StartFailed(ex)
    }

    def running(binding: ServerBinding): Behavior[Message] =
      Behaviors.receiveMessagePartial[Message] {
        case Stop =>
          ctx.log.info(
            "Stopping server http://{}:{}/",
            binding.localAddress.getHostString,
            binding.localAddress.getPort)
          Behaviors.stopped
      }.receiveSignal {
        case (_, PostStop) =>
          binding.unbind()
          Behaviors.same
      }

    def starting(wasStopped: Boolean): Behaviors.Receive[Message] =
      Behaviors.receiveMessage[Message] {
        case StartFailed(cause) =>
          throw new RuntimeException("Server failed to start", cause)
        case Started(binding) =>
          ctx.log.info(
            "Server online at http://{}:{}/",
            binding.localAddress.getHostString,
            binding.localAddress.getPort)
          if (wasStopped) ctx.self ! Stop
          running(binding)
        case Stop =>
          // we got a stop message but haven't completed starting yet,
          // we cannot stop until starting has completed
          starting(wasStopped = true)
      }

    starting(wasStopped = false)
  }
}

object LowLevelRest extends App {
  val system: ActorSystem[Server.Message] = ActorSystem(Server("localhost", 9000), "LowLevelRestServer")
}

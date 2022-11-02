package part3_highlevelserver

import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, PostStop}
import akka.actor.typed.scaladsl.Behaviors
import akka.event.LoggingAdapter
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import part3_highlevelserver.GuitarDB.FindAllGuitars
import spray.json.DefaultJsonProtocol._
import spray.json._
import spray.json.RootJsonFormat

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.io.StdIn
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

  // Seeding with values since actor is a FSM
  def apply(): Behavior[Command] = state(
    4,
    Map(
      (1, Guitar("Fender", "Stratocaster", 3)),
      (2, Guitar("Gibson", "Les Paul", 10)),
      (3, Guitar("Martin", "LX1", 0)),
    )
  )

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
    import part3_highlevelserver.GuitarDB._

    implicit val system: ActorSystem[Nothing] = ctx.system
    implicit val executionContext: ExecutionContextExecutor = system.executionContext
    implicit val timeout: Timeout = Timeout(2 seconds)

    val guitarsDb = ctx.spawn(GuitarDB(), "GuitarsDB")

    val guitarServerRoute: Route = {
      concat(
        path("api" / "guitar") {
          concat(
            // ALWAYS PUT THE MORE SPECIFIC ROUTE FIRST
            parameter("id".as[Int]) { (guitarId: Int) =>
              get {
                val getGuitarFuture = guitarsDb.ask(FindGuitar(guitarId, _)).mapTo[Option[Guitar]]
                val entityFuture = getGuitarFuture.map { guitar =>
                  HttpEntity(
                    ContentTypes.`application/json`,
                    guitar.toJson.prettyPrint
                  )
                }
                complete(entityFuture)
              }
            },
            get {
              val guitarsFuture = (guitarsDb ? FindAllGuitars).mapTo[List[Guitar]]
              val entityFuture = guitarsFuture.map { guitars =>
                HttpEntity(
                  ContentTypes.`application/json`,
                  guitars.toJson.prettyPrint
                )
              }
              complete(entityFuture)
            }
          )
        },
        path("api" / "guitar" / IntNumber) { guitarId =>
          get {
            val getGuitarFuture = guitarsDb.ask(FindGuitar(guitarId, _)).mapTo[Option[Guitar]]
            val entityFuture = getGuitarFuture.map { guitar =>
              HttpEntity(
                ContentTypes.`application/json`,
                guitar.toJson.prettyPrint
              )
            }
            complete(entityFuture)
          }
        },
        path("api" / "guitar" / "inventory") {
          get {
            parameter("inStock".as[Boolean]) { (inStock: Boolean) =>
              val guitarsFuture = guitarsDb.ask(FindGuitarsByStock(inStock, _)).mapTo[List[Guitar]]
              val entityFuture = guitarsFuture.map { guitars =>
                HttpEntity(
                  ContentTypes.`application/json`,
                  guitars.toJson.prettyPrint
                )
              }
              complete(entityFuture)
            }
          }
        }
      )
    }

    def toHttpEntity(payload: String) = HttpEntity(ContentTypes.`application/json`, payload)

    val simpleGuitarServerRoute: Route =
      (pathPrefix("api" / "guitar") & get) {
        concat(
          path("inventory") {
            parameter("inStock".as[Boolean]) { (inStock: Boolean) =>
              complete(guitarsDb
                .ask(FindGuitarsByStock(inStock, _))
                .mapTo[List[Guitar]]
                .map(_.toJson.prettyPrint)
                .map(toHttpEntity)
              )
            }
          },
          (path(IntNumber) | parameter("id".as[Int])) { guitarId =>
            complete(guitarsDb
              .ask(FindGuitar(guitarId, _))
              .mapTo[Option[Guitar]]
              .map(_.toJson.prettyPrint)
              .map(toHttpEntity)
            )
          },
          pathEndOrSingleSlash {
            complete(guitarsDb
              .ask(FindAllGuitars)
              .mapTo[List[Guitar]]
              .map(_.toJson.prettyPrint)
              .map(toHttpEntity)
            )
          }
        )
      }

    val serverBinding = Http().newServerAt(host, port).bind(simpleGuitarServerRoute)
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

object HighLevelExample extends App {
  val system: ActorSystem[Server.Message] = ActorSystem(Server("localhost", 9000), "HighLevelExample")
}

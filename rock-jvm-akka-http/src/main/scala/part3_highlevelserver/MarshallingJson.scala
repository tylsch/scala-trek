package part3_highlevelserver

import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, PostStop}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.io.StdIn
import scala.language.postfixOps
import scala.util.{Failure, Success}

case class Player(nickName: String, characterClass: String, level: Int)

object GameAreaMap {
  trait Command
  case class GetAllPlayers(replyTo: ActorRef[List[Player]]) extends Command
  case class GetPlayer(nickName: String, replyTo: ActorRef[Option[Player]]) extends Command
  case class GetPlayerByClass(characterClass: String, replyTo: ActorRef[List[Player]]) extends Command
  case class AddPlayer(player: Player, replyTo: ActorRef[OperationSuccess]) extends Command
  case class RemovePlayer(player: Player, replyTo: ActorRef[OperationSuccess]) extends Command

  trait Event
  case class OperationSuccess(nickName: String) extends Event

  def apply(): Behavior[Command] = state(
    Map(
      "martin_killz_u" -> Player("martin_killz_u", "Warrior", 70),
      "rolandbraveheart" -> Player("rolandbraveheart", "Elf", 67),
      "daniel_rock03" -> Player("daniel_rock03", "Wizard", 30),
    )
  )

  def state(players: Map[String, Player]): Behavior[Command] = Behaviors.receive { (ctx, msg) =>
    msg match {
      case GetAllPlayers(replyTo) =>
        ctx.log.info("Getting all players")
        replyTo ! players.values.toList
        Behaviors.same
      case GetPlayer(nickName, replyTo) =>
        ctx.log.info(s"Getting player by nickname $nickName")
        replyTo ! players.get(nickName)
        Behaviors.same
      case GetPlayerByClass(characterClass, replyTo) =>
        ctx.log.info(s"Getting all players with the character class $characterClass")
        replyTo ! players.values.filter(_.characterClass == characterClass).toList
        Behaviors.same
      case AddPlayer(player, replyTo) =>
        ctx.log.info(s"Adding the following player: $player")
        replyTo ! OperationSuccess(player.nickName)
        state(players + (player.nickName -> player))
      case RemovePlayer(player, replyTo) =>
        ctx.log.info(s"Removing the following player: $player")
        replyTo ! OperationSuccess(player.nickName)
        state(players - player.nickName)
      case _ => Behaviors.same
    }
  }
}

object ServerMarshalling {

  sealed trait Message
  private final case class StartFailed(cause: Throwable) extends Message
  private final case class Started(binding: ServerBinding) extends Message
  case object Stop extends Message

  def apply(host: String, port: Int): Behavior[Message] = Behaviors.setup { ctx =>
    import akka.actor.typed.scaladsl.AskPattern.schedulerFromActorSystem
    import part3_highlevelserver.GameAreaMap._

    implicit val system: ActorSystem[Nothing] = ctx.system
    implicit val executionContext: ExecutionContextExecutor = system.executionContext
    implicit val timeout: Timeout = Timeout(2 seconds)
    implicit val playerFormat: RootJsonFormat[Player] = jsonFormat3(Player)

    val gameAreaMap = ctx.spawn(GameAreaMap(), "GameAreaMap")

    val routes: Route =
      pathPrefix("api" / "player") {
        concat(
          get {
            concat(
              path("class" / Segment) { characterClass =>
                val playersByClassFuture = gameAreaMap.ask(GetPlayerByClass(characterClass, _)).mapTo[List[Player]]
                complete(playersByClassFuture)
              },
              (path(Segment) | parameter("nickName")) { nickName =>
                val playerByNicknameFuture = gameAreaMap.ask(GetPlayer(nickName, _)).mapTo[Option[Player]]
                complete(playerByNicknameFuture)
              },
              pathEndOrSingleSlash {
                complete(gameAreaMap.ask(GetAllPlayers).mapTo[List[Player]])
              }
            )
          },
          post {
            entity(as[Player]) { player =>
              complete(gameAreaMap.ask(AddPlayer(player, _)).map(_ => StatusCodes.OK))
            }
          },
          delete {
            entity(as[Player]) { player =>
              complete(gameAreaMap.ask(RemovePlayer(player, _)).map(_ => StatusCodes.OK))
            }
          }
        )
      }

    val serverBinding = Http().newServerAt(host, port).bind(routes)
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

object MarshallingJson extends App {
  val system: ActorSystem[ServerMarshalling.Message] = ActorSystem(ServerMarshalling("localhost", 8080), "MarshallingJson")
}

package part2_lowlevelserver

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._

import scala.concurrent.ExecutionContextExecutor

case class Guitar(make: String, model: String)

trait Command
case class CreateGuitar(guitar: Guitar, replyTo: ActorRef[GuitarCreated]) extends Command
case class FindGuitar(id: Int, replyTo: ActorRef[Option[Guitar]]) extends Command
case class FindAllGuitars(replyTo: ActorRef[List[Guitar]]) extends Command

trait Event
case class GuitarCreated(id: Int) extends Event

object GuitarDB {
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
      case CreateGuitar(guitar, replyTo) =>
        ctx.log.info(s"Adding guitar $guitar with id $currentId")
        replyTo ! GuitarCreated(currentId)
        state(currentId + 1, guitars + (currentId -> guitar))
      case _ => Behaviors.same
    }
  }
}

object LowLevelRest extends App {
  implicit val system: ActorSystem[Command] = ActorSystem(GuitarDB(), "LowLevelRest")
  implicit val executionContext: ExecutionContextExecutor = system.executionContext

  val route =
    path("/api/guitar") {
      get {
        complete(HttpEntity(ContentTypes.`application/json`, "<h1>Say hello to akka-http</h1>"))
      }
    }

  val bindingFuture = Http().newServerAt("localhost", 8080).bind(route)

  println(s"Server now online. Please navigate to http://localhost:8080/hello\nPress RETURN to stop...")
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done
}

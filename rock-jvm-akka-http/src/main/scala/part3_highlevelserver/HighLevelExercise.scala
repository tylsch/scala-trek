package part3_highlevelserver

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import spray.json._
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.io.StdIn
import scala.language.postfixOps
import scala.util.{Failure, Success}

case class Person(pin: Int, name: String)

trait PersonJsonSupport extends SprayJsonSupport {
  implicit val guitarFormat: RootJsonFormat[Person] = jsonFormat2(Person)
}

object HighLevelExercise extends App with PersonJsonSupport {
  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "HighLevelExercise")
  implicit val executionContext: ExecutionContextExecutor = system.executionContext

  var people = List(
    Person(1, "Alice"),
    Person(2, "Bob"),
    Person(3, "Charlie")
  )

  def toHttpEntity(payload: String): HttpEntity.Strict = HttpEntity(ContentTypes.`application/json`, payload)

  val routes: Route =
    pathPrefix("api" / "people") {
      concat(
        get {
          concat(
            (path(IntNumber) | parameter("pin".as[Int])) { pin =>
              complete(toHttpEntity(people.find(_.pin == pin).toJson.prettyPrint))
            },
            pathEndOrSingleSlash {
              complete(toHttpEntity(people.toJson.prettyPrint))
            }
          )
        },
        (post & pathEndOrSingleSlash & extractRequest & extractLog) { (request, log) =>
          val entity = request.entity
          val strictEntityFuture = entity.toStrict(2 seconds)
          val personFuture = strictEntityFuture.map(_.data.utf8String.parseJson.convertTo[Person])

          onComplete(personFuture) {
            case Success(person) =>
              log.info(s"Got person: $person")
              people = people :+ person
              complete(StatusCodes.OK)
            case Failure(exception) =>
              log.warning(s"Something failed with fetching teh person from the entity: $exception")
              failWith(exception)
          }
//          personFuture.onComplete {
//            case Success(person) =>
//              log.info(s"Got person: $person")
//              people = people :+ person
//            case Failure(exception) =>
//              log.warning(s"Something failed with fetching teh person from the entity: $exception")
//          }
//
//          complete(personFuture.map(_ => StatusCodes.OK).recover {
//            case _ => StatusCodes.InternalServerError
//          })
        }
      )
    }

  val bindingFuture: Future[Http.ServerBinding] = Http().newServerAt("localhost", 8080).bind(routes)
  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done
}
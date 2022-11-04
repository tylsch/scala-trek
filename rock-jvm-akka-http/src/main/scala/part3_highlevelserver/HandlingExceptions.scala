package part3_highlevelserver

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.io.StdIn

object HandlingExceptions extends App {
  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "HandlingExceptions")
  implicit val executionContext: ExecutionContextExecutor = system.executionContext

  val simpleRoute: Route = Route
    .seal(
      path("api" / "people") {
        concat(
          get {
            throw new RuntimeException("Getting all the people took too long")
          },
          post {
            parameter("id") { id =>
              if (id.length > 2)
                throw new NoSuchElementException(s"Parameter $id cannot be found in the database, TABLE FLIP!")

              complete(StatusCodes.OK)
            }
          }
        )
      }
    )


  implicit val customExceptionHandler: ExceptionHandler = ExceptionHandler {
    case e: RuntimeException =>
      complete(StatusCodes.NotFound, e.getMessage)
    case e: IllegalArgumentException =>
      complete(StatusCodes.BadRequest, e.getMessage)
  }

  val runtimeExceptionHandler: ExceptionHandler = ExceptionHandler {
    case e: RuntimeException =>
      complete(StatusCodes.NotFound, e.getMessage)
  }

  val noSuchElementExceptionHandler: ExceptionHandler = ExceptionHandler {
    case e: NoSuchElementException =>
      complete(StatusCodes.BadRequest, e.getMessage)
  }

  val delicateHandleRoute: Route = {
    handleExceptions(runtimeExceptionHandler) {
      path("api" / "people") {
        concat(
          get {
            throw new RuntimeException("Getting all the people took too long")
          },
          handleExceptions(noSuchElementExceptionHandler) {
            post {
              parameter("id") { id =>
                if (id.length > 2)
                  throw new NoSuchElementException(s"Parameter $id cannot be found in the database, TABLE FLIP!")

                complete(StatusCodes.OK)
              }
            }
          }
        )
      }
    }
  }

  val bindingFuture: Future[Http.ServerBinding] = Http().newServerAt("localhost", 8080).bind(delicateHandleRoute)
  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done
}

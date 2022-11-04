package part3_highlevelserver

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{MethodRejection, MissingQueryParamRejection, Rejection, RejectionHandler, Route}
import com.sun.tools.sjavac.server.RequestHandler

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.io.StdIn

object HandlingRejections extends App {
  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "HandlingRejections")
  implicit val executionContext: ExecutionContextExecutor = system.executionContext

  val simpleRoute: Route =
    path("api" / "myEndpoint") {
      concat(
        get {
          complete(StatusCodes.OK)
        },
        parameter("id") { _ =>
          complete(StatusCodes.OK)
        }
      )
    }

  // Rejection Handlers
  val badRequestHandler: RejectionHandler = { rejections: Seq[Rejection] =>
    println(s"I have encountered rejections: $rejections")
    Some(complete(StatusCodes.BadRequest))
  }

  val forbiddenHandler: RejectionHandler = { rejections: Seq[Rejection] =>
    println(s"I have encountered rejections: $rejections")
    Some(complete(StatusCodes.Forbidden))
  }

  val simpleRouteWithHandlers: Route =
    handleRejections(badRequestHandler) {
      path("api" / "myEndpoint") {
        concat(
          get {
            complete(StatusCodes.OK)
          },
          post {
            handleRejections(forbiddenHandler) {
              parameter("id") { _ =>
                complete(StatusCodes.OK)
              }
            }
          }
        )
      }
    }

  val customRejectionHandler: RejectionHandler =
    RejectionHandler.newBuilder()
      .handle {
        case MissingQueryParamRejection(parameterName) =>
          println(s"I got a query param rejection: $parameterName")
          complete("Rejected query parameter!")
      }
      .handle {
        case MethodRejection(supported) =>
          println(s"I got a method rejection: $supported")
          complete("Rejected method!")
      }
      .result()

  val rejectionWrapper: Route =
    handleRejections(customRejectionHandler) {
      simpleRoute
    }

  val bindingFuture: Future[Http.ServerBinding] = Http().newServerAt("localhost", 8080).bind(rejectionWrapper)
  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done
}

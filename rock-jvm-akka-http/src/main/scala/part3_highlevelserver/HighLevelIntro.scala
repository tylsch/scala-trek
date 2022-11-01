package part3_highlevelserver

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.io.StdIn

object HighLevelIntro extends App {
  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "HighLevelIntro")
  implicit val executionContext: ExecutionContextExecutor = system.executionContext

  // Directives
  val simpleRoute: Route =
    path("home") {
      complete(StatusCodes.OK)
    }

  val pathGetRoute: Route =
    path("home") {
      get {
        complete(StatusCodes.OK)
      }
    }

  // chaining directives with ~
  val chainedRoute: Route =
    path("myEndpoint") {
      get {
        complete(StatusCodes.OK)
      } ~ // VERY IMPORTANT
      post {
        complete(StatusCodes.Forbidden)
      }
    } ~
      path("home") {
        complete(
          HttpEntity(
            ContentTypes.`text/html(UTF-8)`,
            """
              |<html>
              |  <body>
              |    Hello from the high level Akka HTTP!
              |  </body>
              |</html>
              |""".stripMargin
          )
        )
      } // Routing Tree

  val bindingFuture: Future[Http.ServerBinding] = Http().newServerAt("localhost", 8080).bind(chainedRoute)
  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done
}

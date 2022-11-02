package part3_highlevelserver

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.event.LoggingAdapter
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.io.StdIn

object DirectivesBreakdown extends App {
  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "DirectiveBreakdown")
  implicit val executionContext: ExecutionContextExecutor = system.executionContext

  /*
  * Type #1: Filtering Directives
  * */
  val simpleHttpMethodRoute: Route =
    post { // equivalent directives for GET, PUT, PATCH, DELETE, HEAD, OPTIONS
      complete(StatusCodes.Forbidden)
    }

  val simplePathRoute: Route =
    path("about") {
      complete(
        HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            |  <body>
            |    Hello from about page
            |  </body>
            |</html>
            |""".stripMargin
        )
      )
    }

  val complexPathRoute: Route =
    path("api" / "myEndpoint") {
      complete(StatusCodes.OK)
    }

  val dontConfuse: Route =
    path("api/myEndpoint") {
      complete(StatusCodes.OK)
    }

  val pathEndRoute: Route =
    pathEndOrSingleSlash { // localhost:8080 or localhost:8080/
      complete(StatusCodes.OK)
    }

  /*
  * Type #2: Extraction Directives
  * */

  val pathExtractionRoute =
    path("api" / "item" / IntNumber) { (itemNumber: Int) =>
      println(s"I've got a number in my path: $itemNumber")
      complete(StatusCodes.OK)
    }

  val pathMultiExtractRoute: Route =
    path("api" / "order" / IntNumber / IntNumber) { (id, inventory) =>
      println(s"I've got TWO numbers in my path: $id & $inventory")
      complete(StatusCodes.OK)
    }

  val queryParamExtractionRoute: Route =
    path("api" / "item") {
      parameter("id".as[Int]) { (itemId: Int) =>
        println(s"I've extracted the ID as $itemId")
        complete(StatusCodes.OK)
      }
    }

  val extractRequestRoute: Route =
    path("controlEndpoint") {
      extractRequest { (httpRequest: HttpRequest) =>
        extractLog { (log: LoggingAdapter) =>
          log.info(s"I got the HTTP request: $httpRequest")
          complete(StatusCodes.OK)
        }
      }
    }

  /*
  * Type #3: Composite Directives
  * */

  val simpleNestedRoute: Route =
    path("api" / "item") {
      get {
        complete(StatusCodes.OK)
      }
    }

  val compactSimpleNestedRoute = (path("api" / "item") & get) {
    complete(StatusCodes.OK)
  }

  val compactExtractRequestRoute: Route =
    (path("controlEndpoint") & extractRequest & extractLog) {
      (request: HttpRequest, log: LoggingAdapter) =>
        log.info(s"I got the HTTP request: $request")
        complete(StatusCodes.OK)
    }

  // /about and /aboutUs
  val repeatedRoute: Route =
    path("about") {
      complete(StatusCodes.OK)
    } ~
    path ("aboutUs") {
      complete(StatusCodes.OK)
    }

  val dryRoute: Route =
    (path("about") | path("aboutUs")) {
      complete(StatusCodes.OK)
    }

  // yourblog.com/42 AND yourblog.com?postId=42
  val blogByIdRoute: Route = {
    path(IntNumber) { (blogId: Int) =>
      complete(StatusCodes.OK)
    }
  }

  val blogByQueryParamRoute: Route =
    parameter("postId".as[Int]) { (blogPostId: Int) =>
      complete(StatusCodes.OK)
    }

  val combinedBlogByIdRoute: Route =
    (path(IntNumber) | parameter("postId".as[Int])) { (blogPostId: Int) =>
      complete(StatusCodes.OK)
    }

  /*
  * Type #4: "Actionable" directives
  * */

  val completeOkRoute: Route = complete(StatusCodes.OK)

  val failedRoute: Route =
    path("notSupported") {
      failWith(new RuntimeException("Unsupported")) // completes with HTTP 500
    }

  val routeWithRejection: Route =
    path("home") {
      reject
    } ~
      path("index") {
        completeOkRoute
      }

  val getOrPutPath: Route =
    path("api" / "myEndpoint") {
      get {
        completeOkRoute
      } ~
      post {
        complete(StatusCodes.Forbidden)
      }
    }

  val bindingFuture: Future[Http.ServerBinding] = Http().newServerAt("localhost", 8080).bind(queryParamExtractionRoute)
  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done
}
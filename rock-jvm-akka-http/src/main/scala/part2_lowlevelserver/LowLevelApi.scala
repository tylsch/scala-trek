package part2_lowlevelserver

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.IncomingConnection
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.stream.scaladsl.{Flow, Sink}

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.{Failure, Success}

object LowLevelApi extends App {
  implicit val system: ActorSystem = ActorSystem("LowLevelApi")
  import system.dispatcher

  val serverSource = Http().newServerAt("localhost", 8000).connectionSource()
  val connectionSink = Sink.foreach[IncomingConnection] { connection =>
    println(s"Accepted incoming connection from: ${connection.remoteAddress}")
  }

  val serverBindingFuture = serverSource.to(connectionSink).run()
  serverBindingFuture.onComplete {
    case Success(value) =>
      println(s"Server binding successful")
      value.terminate(2 seconds)
    case Failure(exception) => println(s"Server binding failed: $exception")
  }

  /*
  * Method 1: synchronously serve HTTP responses
  * */

  val requestHandler: HttpRequest => HttpResponse = {
    case HttpRequest(HttpMethods.GET, _, _, _, _) =>
      HttpResponse(
        StatusCodes.OK,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            |  <body>
            |    Hello from Akka HTTP!
            |  </body>
            |</html>
            |""".stripMargin
        )
      )
    case request:  HttpRequest =>
      request.discardEntityBytes()
      HttpResponse(
        StatusCodes.NotFound,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            |  <body>
            |    Resource can't be found
            |  </body>
            |</html>
            |""".stripMargin
        )
      )
  }

  val httpSyncConnectionHandler = Sink.foreach[IncomingConnection] { connection =>
    connection.handleWithSyncHandler(requestHandler)
  }

  //Http().newServerAt("localhost", 8080).connectionSource().runWith(httpSyncConnectionHandler)
  // shorthand version
  //Http().newServerAt("localhost", 8080).bindSync(requestHandler)

  /*
  * Method 2: serve back HTTP responses ASYNCHRONOUSLY
  * */

  val asyncRequestHandler: HttpRequest => Future[HttpResponse] = {
    case HttpRequest(HttpMethods.GET, Uri.Path("/home"), _, _, _) =>
      Future(HttpResponse(
        StatusCodes.OK,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            |  <body>
            |    Hello from Akka HTTP!
            |  </body>
            |</html>
            |""".stripMargin
        )
      ))
    case request: HttpRequest =>
      request.discardEntityBytes()
      Future(HttpResponse(
        StatusCodes.NotFound,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            |  <body>
            |    Resource can't be found
            |  </body>
            |</html>
            |""".stripMargin
        )
      ))
  }

  val httpAsyncConnectionHandler = Sink.foreach[IncomingConnection] { connection =>
    connection.handleWithAsyncHandler(asyncRequestHandler)
  }

  //Http().newServerAt("localhost", 8081).connectionSource().runWith(httpAsyncConnectionHandler)
  // shorthand version
  //Http().newServerAt("localhost", 8081).bind(asyncRequestHandler)

  /*
  * Method 3: async via Akka Streams
  * */
  val streamsBasedRequestHandler: Flow[HttpRequest, HttpResponse, _] = Flow[HttpRequest].map {
    case HttpRequest(HttpMethods.GET, Uri.Path("/home"), _, _, _) =>
      HttpResponse(
        StatusCodes.OK,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            |  <body>
            |    Hello from Akka HTTP via Akka Streams!
            |  </body>
            |</html>
            |""".stripMargin
        )
      )
    case request: HttpRequest =>
      request.discardEntityBytes()
      HttpResponse(
        StatusCodes.NotFound,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            |  <body>
            |    Resource can't be found
            |  </body>
            |</html>
            |""".stripMargin
        )
      )
  }

//  Http().newServerAt("localhost", 8082).connectionSource().runForeach { connection =>
//    connection.handleWith(streamsBasedRequestHandler)
//  }
  // shorthand version
  //Http().newServerAt("localhost", 8082).bindFlow(streamsBasedRequestHandler)

  /*
  * Exercise: create your own HTTP Server running on localhost on 8388, which replies
  * - with a welcome message on the "front door" localhost:8388
  * - with a proper HTML on localhost:8388/about
  * - with a 404 message otherwise
  * */

  val syncExerciseHandler: HttpRequest => HttpResponse = {
    case HttpRequest(HttpMethods.GET, Uri.Path("/"), _, _, _) =>
      HttpResponse(
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          "Hello from the exercise front door!"
        )
      )
    case HttpRequest(HttpMethods.GET, Uri.Path("/search"), _, _, _) =>
      HttpResponse(
        StatusCodes.Found,
        headers = List(Location("https://google.com"))
      )
    case HttpRequest(HttpMethods.GET, Uri.Path("/about"), _, _, _) =>
      HttpResponse(
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            |  <body>
            |    <div style="color: red">Hello from about page!</div>
            |  </body>
            |</html>
            |""".stripMargin
        )
      )
    case request: HttpRequest =>
      request.discardEntityBytes()
      HttpResponse(
        StatusCodes.NotFound,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          "OOPS, you're in no man's land, sorry"
        )
      )
  }

  val bindingFuture = Http().newServerAt("localhost", 8388).bindSync(syncExerciseHandler)
  // shutdown server
  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())

}

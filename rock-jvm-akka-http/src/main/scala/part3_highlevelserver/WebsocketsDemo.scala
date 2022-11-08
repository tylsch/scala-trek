package part3_highlevelserver

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.CompactByteString

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.io.StdIn
import scala.language.postfixOps

object WebsocketsDemo extends App {
  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "WebsocketDemo")
  implicit val executionContext: ExecutionContextExecutor = system.executionContext

  val textMessage = TextMessage(Source.single("hello via a text message"))
  val binaryMessage = BinaryMessage(Source.single(CompactByteString("hello via a binary message")))

  val html =
    """
      |<html>
      |    <head>
      |        <script>
      |            var exampleSocket = new WebSocket("ws://localhost:8080/greeter")
      |            console.log("starting websocket...");
      |
      |            exampleSocket.onmessage = function (event) {
      |                var newChild = document.createElement("div");
      |                newChild.innerText = event.data;
      |                document.getElementById("1").appendChild(newChild);
      |            };
      |            exampleSocket.onopen = function (event) {
      |                exampleSocket.send("socket seems to be open...");
      |            };
      |
      |            exampleSocket.send("socket says: hello, server!");
      |        </script>
      |    </head>
      |    <body>
      |        Starting websocket...
      |        <div id="1"></div>
      |    </body>
      |</html>
      |""".stripMargin

  val websocketFlow: Flow[Message, Message, Any] = Flow[Message].map {
    case tm: TextMessage =>
      TextMessage(Source.single("Server says back:") ++ tm.textStream ++ Source.single("!"))
    case bm: BinaryMessage =>
      bm.dataStream.runWith(Sink.ignore)
      TextMessage(Source.single("Server received a binary message..."))
  }

  case class SocialPost(owner: String, content: String)
  val socialFeed = Source(
    List(
      SocialPost("Martin", "Scala 3 has been announced!"),
      SocialPost("Daniel", "New RTJVM course"),
      SocialPost("Martin", "I killed Java!")
    )
  )

  val socialMessages = socialFeed
    .throttle(1, 2 seconds)
    .map((socialPost => TextMessage(s"${socialPost.owner} said: ${socialPost.content}")))

  val socialFlow: Flow[Message, Message, Any] = Flow.fromSinkAndSource(
    Sink.foreach[Message](println),
    socialMessages
  )

  val simpleRoute: Route =
    concat(
      (pathEndOrSingleSlash & get) {
        complete(
          HttpEntity(
            ContentTypes.`text/html(UTF-8)`,
            html
          )
        )
      },
      path("greeter") {
        handleWebSocketMessages(socialFlow)
      }
    )

  val bindingFuture: Future[Http.ServerBinding] = Http().newServerAt("localhost", 8080).bind(simpleRoute)
  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done
}

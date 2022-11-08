package part3_highlevelserver

import akka.Done
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, Multipart}
import akka.http.scaladsl.model.Multipart.FormData
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.IOResult
import akka.stream.scaladsl.{FileIO, Sink, Source}
import akka.util.ByteString

import java.io.File
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.io.StdIn
import scala.util.{Failure, Success}

object FileUpload extends App {
  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "FileUpload")
  implicit val executionContext: ExecutionContextExecutor = system.executionContext

  val simpleRoute: Route =
    concat(
      (pathEndOrSingleSlash & get) {
        complete(
          HttpEntity(
            ContentTypes.`text/html(UTF-8)`,
            """
              |<html>
              |  <body>
              |    <form action="/upload" method="post" enctype="multipart/form-data">
              |      <input type="file" name="myFile"></input>
              |      <button type="submit">Upload</button>
              |    </form>
              |  </body>
              |</html>
              |""".stripMargin
          )
        )
      },
      (path("upload") & extractLog) { log =>
        entity(as[Multipart.FormData]) { formData =>
          val partsSource: Source[FormData.BodyPart, Any] = formData.parts
          val filePartsSink: Sink[FormData.BodyPart, Future[Done]] = Sink.foreach[FormData.BodyPart] { bodyPart =>
            if (bodyPart.name == "myFile") {
              val fileName = "src/main/resources/download/" + bodyPart.filename.getOrElse("tempFile_" + System.currentTimeMillis())
              val file = new File(fileName)

              log.info(s"Writing to file: $fileName")

              val fileContentsSource: Source[ByteString, Any] = bodyPart.entity.dataBytes
              val fileContentsSink: Sink[ByteString, Future[IOResult]] = FileIO.toPath(file.toPath)

              fileContentsSource.runWith(fileContentsSink)
            }
          }

          val writeOperationFuture = partsSource.runWith(filePartsSink)
          onComplete(writeOperationFuture) {
            case Success(_) => complete("File uploaded")
            case Failure(exception) => complete(s"File failed to upload: $exception")
          }
        }
      }
    )

  val bindingFuture: Future[Http.ServerBinding] = Http().newServerAt("localhost", 8080).bind(simpleRoute)
  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done
}

package part4_client

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.scaladsl.{Sink, Source}
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

object RequestLevel extends App {
  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "RequestLevel")
  implicit val executionContext: ExecutionContextExecutor = system.executionContext
  implicit val creditCardFormat: RootJsonFormat[CreditCard] = jsonFormat3(CreditCard)
  implicit val postPaymentRequestFormat: RootJsonFormat[PostPaymentRequest] = jsonFormat3(PostPaymentRequest)

//  val responseFuture = Http().singleRequest(HttpRequest(uri = "http://www.google.com"))
//  responseFuture.onComplete {
//    case Success(value) =>
//      value.discardEntityBytes()
//      println(s"The request was successful and returned: $value")
//    case Failure(exception) =>
//      println(s"The request failed with exception: $exception")
//  }

  val creditCards = List(
    CreditCard("4242-4242-4242-4242", "424", "test-account"),
    CreditCard("1234-1234-1234-1234", "123", "daniels-account"),
    CreditCard("1234-1234-4321-1234", "432", "awesome-account")
  )

  val paymentRequests = creditCards.map(creditCard => PostPaymentRequest(creditCard, "rtjvm-store-account", 99))
  val serverHttpRequests = paymentRequests.map(paymentRequest =>
    HttpRequest(
      HttpMethods.POST,
      uri = Uri("http://localhost:8080/api/payments"),
      entity = HttpEntity(
        ContentTypes.`application/json`,
        paymentRequest.toJson.prettyPrint
      )
    )
  )

  Source(serverHttpRequests)
    .mapAsyncUnordered(10)(request => Http().singleRequest(request))
    .runForeach(println)
}

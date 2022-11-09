package part4_client

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.scaladsl.{Sink, Source}
import spray.json.DefaultJsonProtocol._
import spray.json._

import java.util.UUID
import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

object HostLevel extends App {
  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "ConnectionLevel")
  implicit val executionContext: ExecutionContextExecutor = system.executionContext
  implicit val creditCardFormat: RootJsonFormat[CreditCard] = jsonFormat3(CreditCard)
  implicit val postPaymentRequestFormat: RootJsonFormat[PostPaymentRequest] = jsonFormat3(PostPaymentRequest)

//  val poolFlow = Http().cachedHostConnectionPool[Int]("www.google.com")
//
//  Source(1 to 10)
//    .map(i => (HttpRequest(), i))
//    .via(poolFlow)
//    .map {
//      case (Success(response), value) =>
//        response.discardEntityBytes()
//        s"Request $value has received response: $response"
//      case (Failure(ex), value) =>
//        s"Request $value has failed: $ex"
//    }
//    .runWith(Sink.foreach[String](println))

  /*
  * A small payment system
  * */
  val creditCards = List(
    CreditCard("4242-4242-4242-4242", "424", "test-account"),
    CreditCard("1234-1234-1234-1234", "123", "daniels-account"),
    CreditCard("1234-1234-4321-1234", "432", "awesome-account")
  )

  val paymentRequests = creditCards.map(creditCard => PostPaymentRequest(creditCard, "rtjvm-store-account", 99))
  val serverHttpRequests = paymentRequests.map(paymentRequest =>
    (
      HttpRequest(
        HttpMethods.POST,
        uri = Uri("/api/payments"),
        entity = HttpEntity(
          ContentTypes.`application/json`,
          paymentRequest.toJson.prettyPrint
        )
      ),
      UUID.randomUUID().toString
    )
  )

  // Host-Level Use Case: High-Volume & Low-Latency Requests
  Source(serverHttpRequests)
    .via(Http().cachedHostConnectionPool[String]("localhost", 8080))
    .runForeach {
      case (Success(response@HttpResponse(StatusCodes.Forbidden, _, _, _)), orderId) =>
        println(s"The order ID $orderId was not allowed to proceed: $response")
      case (Success(response), orderId) =>
        println(s"The order ID $orderId was successful and returned the response: $response")
      case (Failure(ex), orderId) =>
        println(s"The order ID $orderId could not be completed: $ex")
    }
}

package part4_client

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, Uri}
import akka.stream.scaladsl.{Sink, Source}
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}


object ConnectionLevel extends App {
  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "ConnectionLevel")
  implicit val executionContext: ExecutionContextExecutor = system.executionContext
  implicit val creditCardFormat: RootJsonFormat[CreditCard] = jsonFormat3(CreditCard)
  implicit val postPaymentRequestFormat: RootJsonFormat[PostPaymentRequest] = jsonFormat3(PostPaymentRequest)

//  val connectionFlow = Http().outgoingConnection("www.google.com")
//
//  def oneOffRequest(request: HttpRequest) =
//    Source.single(request).via(connectionFlow).runWith(Sink.head)
//
//  oneOffRequest(HttpRequest()).onComplete {
//    case Success(response) => println(s"Got successful response: $response")
//    case Failure(exception) => println(s"Sending the request failed: $exception")
//  }

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
    HttpRequest(
      HttpMethods.POST,
      uri = Uri("/api/payments"),
      entity = HttpEntity(
        ContentTypes.`application/json`,
        paymentRequest.toJson.prettyPrint
      )
    )
  )

  Source(serverHttpRequests)
    .via(Http().outgoingConnection("localhost", 8080))
    .to(Sink.foreach[HttpResponse](println))
    .run()
}

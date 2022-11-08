package part4_client

import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, PostStop}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.{Failure, Success}

case class CreditCard(serialNumber: String, securityCode: String, account: String)
case class PostPaymentRequest(creditCard: CreditCard, receiver: String, amount: Double)

object PaymentSystemDomain {
  sealed trait Command
  case class PaymentRequest(creditCard: CreditCard, receiver: String, amount: Double, replyTo: ActorRef[Event]) extends Command

  sealed trait Event
  case object PaymentAccepted extends Event
  case object PaymentRejected extends Event

  def apply(): Behavior[Command] = Behaviors.receive { (ctx, msg) =>
    msg match {
      case PaymentRequest(CreditCard(serialNumber, _, account), receiver, amount, replyTo) =>
        ctx.log.info(s"$account is trying to send $amount dollars to $receiver for $serialNumber")
        if (serialNumber == "1234-1234-1234-1234")
          replyTo ! PaymentRejected
        else
          replyTo ! PaymentAccepted

        Behaviors.same
    }
  }
}

object PaymentSystemServer {
  sealed trait Message
  private final case class StartFailed(cause: Throwable) extends Message
  private final case class Started(binding: ServerBinding) extends Message
  case object Stop extends Message

  def apply(host: String, port: Int): Behavior[Message] = Behaviors.setup { ctx =>
    import akka.actor.typed.scaladsl.AskPattern.schedulerFromActorSystem
    import PaymentSystemDomain._

    implicit val system: ActorSystem[Nothing] = ctx.system
    implicit val executionContext: ExecutionContextExecutor = system.executionContext
    implicit val timeout: Timeout = Timeout(2 seconds)
    implicit val creditCardFormat: RootJsonFormat[CreditCard] = jsonFormat3(CreditCard)
    implicit val postPaymentRequestFormat: RootJsonFormat[PostPaymentRequest] = jsonFormat3(PostPaymentRequest)

    val paymentSystemDomain = ctx.spawn(PaymentSystemDomain(), "PaymentSystemDomain")

    val routes: Route =
      path("api" / "payments") {
        post {
          entity(as[PostPaymentRequest]) { pmt =>
            val validationResponse = paymentSystemDomain.ask(PaymentRequest(pmt.creditCard, pmt.receiver, pmt.amount, _)).map {
              case PaymentRejected => StatusCodes.Forbidden
              case PaymentAccepted => StatusCodes.OK
              case _ => StatusCodes.BadRequest
            }
            complete(validationResponse)
          }
        }
      }

    val serverBinding = Http().newServerAt(host, port).bind(routes)
    ctx.pipeToSelf(serverBinding) {
      case Success(binding) => Started(binding)
      case Failure(ex)      => StartFailed(ex)
    }

    def running(binding: ServerBinding): Behavior[Message] =
      Behaviors.receiveMessagePartial[Message] {
        case Stop =>
          ctx.log.info(
            "Stopping server http://{}:{}/",
            binding.localAddress.getHostString,
            binding.localAddress.getPort)
          Behaviors.stopped
      }.receiveSignal {
        case (_, PostStop) =>
          binding.unbind()
          Behaviors.same
      }

    def starting(wasStopped: Boolean): Behaviors.Receive[Message] =
      Behaviors.receiveMessage[Message] {
        case StartFailed(cause) =>
          throw new RuntimeException("Server failed to start", cause)
        case Started(binding) =>
          ctx.log.info(
            "Server online at http://{}:{}/",
            binding.localAddress.getHostString,
            binding.localAddress.getPort)
          if (wasStopped) ctx.self ! Stop
          running(binding)
        case Stop =>
          // we got a stop message but haven't completed starting yet,
          // we cannot stop until starting has completed
          starting(wasStopped = true)
      }

    starting(wasStopped = false)
  }
}

object PaymentSystem extends App {
  val system: ActorSystem[PaymentSystemServer.Message] = ActorSystem(PaymentSystemServer("localhost", 8080), "PaymentSystemServer")
}

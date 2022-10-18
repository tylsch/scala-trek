package part5patterns

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import part5patterns.AskDemo.WorkProtocol
import utils.ActorSystemEnhancements

import java.util.concurrent.{ExecutorService, Executors}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future}
import scala.util.{Failure, Success}

object PipeDemo {

  // interaction with an external service that returns Futures
  val db = Map(
    "Daniel" -> 123,
    "Jane" -> 456,
    "Dee Dee" -> 999
  )

  val executor: ExecutorService = Executors.newFixedThreadPool(4)
  implicit val externalEC: ExecutionContext = ExecutionContext.fromExecutorService(executor)

  def callExternalService(name: String): Future[Int] = {
    Future(db(name))
  }

  trait PhoneCallProtocol
  case class FindAndCallPhoneNumber(name: String) extends PhoneCallProtocol
  case class InitiatePhoneCall(number: Int) extends PhoneCallProtocol
  case class LogPhoneCallFailure(reason: Throwable) extends PhoneCallProtocol

  object PhoneCallActor {
    def apply(): Behavior[PhoneCallProtocol] = Behaviors.receive { (ctx, msg) =>
      msg match {
        case FindAndCallPhoneNumber(name) =>
          ctx.log.info(s"Fetching phone number for $name")
          val phoneNumberFuture = callExternalService(name)
          ctx.pipeToSelf(phoneNumberFuture) {
            case Success(value) => InitiatePhoneCall(value)
            case Failure(exception) => LogPhoneCallFailure(exception)
          }
          Behaviors.same
        case InitiatePhoneCall(number) =>
          ctx.log.info(s"Initiating phone call to $number")
          Behaviors.same
        case LogPhoneCallFailure(reason) =>
          ctx.log.warn(s"Initiating phone call failed: $reason")
          Behaviors.same
      }
    }
  }

  def main(args: Array[String]): Unit = {
    val userGuardian = Behaviors.setup[WorkProtocol] { ctx =>
      val phoneCallActor = ctx.spawn(PhoneCallActor(), "phoneCallActor")

      phoneCallActor ! FindAndCallPhoneNumber("Superman")

      Behaviors.empty
    }

    ActorSystem(userGuardian, "DemoPipePattern").withFiniteLifespan(2.seconds)
  }
}

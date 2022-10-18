package part5patterns

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Scheduler}
import akka.util.Timeout
import utils.ActorSystemEnhancements

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object AskDemo {

  trait WorkProtocol
  case class ComputationalTask(payload: String, replyTo: ActorRef[WorkProtocol]) extends WorkProtocol
  case class ComputationalResult(result: Int) extends WorkProtocol

  object Worker {
    def apply(): Behavior[WorkProtocol] = Behaviors.receive { (ctx, msg) =>
      msg match {
        case ComputationalTask(payload, replyTo) =>
          ctx.log.info(s"[worker] Crunching data for $payload")
          replyTo ! ComputationalResult(payload.split(" ").length)
          Behaviors.same
        case _ => Behaviors.same
      }
    }
  }

  def askSimple(): Unit = {
    import akka.actor.typed.scaladsl.AskPattern._

    val system = ActorSystem(Worker(), "DemoAskSimple").withFiniteLifespan(5.seconds)
    implicit val timeout: Timeout = Timeout(3.seconds)
    implicit val scheduler: Scheduler = system.scheduler

    val reply: Future[WorkProtocol] = system.ask(ref => ComputationalTask("Trying the ask pattern, seems convoluted", ref))

    implicit val ec: ExecutionContext = system.executionContext
    reply.foreach(println)
  }

  def askFromWithinAnotherActor(): Unit = {
    val userGuardian = Behaviors.setup[WorkProtocol] { ctx =>
      val worker = ctx.spawn(Worker(), "worker")

      case class ExtendedComputationalResult(count: Int, description: String) extends WorkProtocol

      implicit val timeout: Timeout = Timeout(3.seconds)

      ctx.ask(worker, ref => ComputationalTask("This ask pattern seems quite complicated", ref)) {
        case Success(ComputationalResult(result)) => ExtendedComputationalResult(result, "This is pretty damn hard")
        case Failure(exception) => ExtendedComputationalResult(-1, s"Computation failed: $exception")
      }

      Behaviors.receiveMessage {
        case ExtendedComputationalResult(count, description) =>
          ctx.log.info(s"Ask and ye shall receive: $description - $count")
          Behaviors.same
        case _ => Behaviors.same
      }
    }

    val system = ActorSystem(userGuardian, "DemoAskConvoluted").withFiniteLifespan(5.seconds)
  }

  def main(args: Array[String]): Unit = {
    askFromWithinAnotherActor()
  }
}

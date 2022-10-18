package part5patterns

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import utils.ActorSystemEnhancements

import scala.concurrent.duration.DurationInt

object StashDemo {

  // an actor with a locked access to a resource
  trait Command
  case object Open extends Command
  case object Close extends Command
  case object Read extends Command
  case class Write(data: String) extends Command

  object ResourceActor {
    def apply(): Behavior[Command] = closed("42")

    def open(data: String): Behavior[Command] = Behaviors.receive { (ctx, msg) =>
      msg match {
        case Read =>
          ctx.log.info(s"I have read $data")
          Behaviors.same
        case Write(data) =>
          ctx.log.info(s"I have written $data")
          open(data)
        case Close =>
          ctx.log.info("Closing resource")
          closed(data)
        case message =>
          ctx.log.info(s"$message not supported while resource is open")
          Behaviors.same
      }
    }

    def closed(data: String): Behavior[Command] = Behaviors.withStash(128) { buffer =>
      Behaviors.receive { (ctx, msg) =>
        msg match {
          case Open =>
            ctx.log.info("Opening Resource")
            buffer.unstashAll(open(data))
          case _ =>
            ctx.log.info(s"Stashing $msg because the resource is closed")
            buffer.stash(msg) // buffer is MUTABLE
            Behaviors.same
        }
      }
    }
  }

  def main(args: Array[String]): Unit = {
    val userGuardian = Behaviors.setup[Unit] { ctx =>
      val resourceActor = ctx.spawn(ResourceActor(), "resourceActor")

      resourceActor ! Read
      resourceActor ! Open
      resourceActor ! Open
      resourceActor ! Write("I love stash")
      resourceActor ! Write("This is cool")
      resourceActor ! Read
      resourceActor ! Read
      resourceActor ! Close
      resourceActor ! Read

      Behaviors.empty
    }

    ActorSystem(userGuardian, "DemoPipePattern").withFiniteLifespan(2.seconds)
  }
}

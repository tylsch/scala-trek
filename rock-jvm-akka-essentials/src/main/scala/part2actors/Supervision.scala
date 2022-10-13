package part2actors

import akka.actor.typed.{ActorSystem, Behavior, SupervisorStrategy, Terminated}
import akka.actor.typed.scaladsl.Behaviors

import scala.concurrent.duration.DurationInt

object Supervision {

  object FussyWordCounter {
    def apply(): Behavior[String] = active()
    def active(total: Int = 0): Behavior[String] = Behaviors.receive { (ctx, msg) =>
      val wordCount = msg.split(" ").length
      ctx.log.info(s"Received piece of art: '$msg', counted $wordCount words, total ${total + wordCount}")
      if (msg.startsWith("Q")) throw new RuntimeException("I HATE QUEUES!!!!")
      if (msg.startsWith("W")) throw new NullPointerException

      active(total + wordCount)
    }
  }

  // actor throwing exception gets killed
  def demoCrash(): Unit = {
    val guardian: Behavior[Unit] = Behaviors.setup { context =>
      val fussyWordCounter = context.spawn(FussyWordCounter(), "fussyCounter")

      fussyWordCounter ! "Starting to understand this Akka business"
      fussyWordCounter ! "Quick! Hide!"
      fussyWordCounter ! "Are you there?"

      Behaviors.empty
    }

    val system = ActorSystem(guardian, "DemoCrash")
    Thread.sleep(1000)
    system.terminate()
  }

  def demoWithParent(): Unit = {
    val parentBehavior: Behavior[String] = Behaviors.setup { context =>
      val child = context.spawn(FussyWordCounter(), "fussyChild")
      context.watch(child)
      Behaviors.receiveMessage[String] { message =>
        child ! message
        Behaviors.same
      }
        .receiveSignal {
          case (context, Terminated(childRef)) =>
            context.log.warn(s"Child failed: ${childRef.path.name}")
            Behaviors.same
        }
    }

    val guardian: Behavior[Unit] = Behaviors.setup { context =>
      val fussyWordCounter = context.spawn(parentBehavior, "fussyCounter")

      fussyWordCounter ! "Starting to understand this Akka business"
      fussyWordCounter ! "Quick! Hide!"
      fussyWordCounter ! "Are you there?"

      Behaviors.empty
    }

    val system = ActorSystem(guardian, "DemoCrash")
    Thread.sleep(1000)
    system.terminate()
  }

  def demoSupervisionWithRestart(): Unit = {
    val parentBehavior: Behavior[String] = Behaviors.setup { context =>
      // supervise the child with a restart "strategy"
      val childBehavior = Behaviors.supervise(
        Behaviors.supervise(FussyWordCounter())
        .onFailure[RuntimeException](SupervisorStrategy.restartWithBackoff(1.second, 1.minute, 0.2))
      ).onFailure[NullPointerException](SupervisorStrategy.resume)

      val child = context.spawn(childBehavior, "fussyChild")
      context.watch(child)
      Behaviors.receiveMessage[String] { message =>
        child ! message
        Behaviors.same
      }
        .receiveSignal {
          case (context, Terminated(childRef)) =>
            context.log.warn(s"Child failed: ${childRef.path.name}")
            Behaviors.same
        }
    }

    val guardian: Behavior[Unit] = Behaviors.setup { context =>
      val fussyWordCounter = context.spawn(parentBehavior, "fussyCounter")

      fussyWordCounter ! "Starting to understand this Akka business"
      fussyWordCounter ! "Quick! Hide!"
      fussyWordCounter ! "Are you there?"
      fussyWordCounter ! "What are you doing?"
      fussyWordCounter ! "Are you still there?"

      Behaviors.empty
    }

    val system = ActorSystem(guardian, "DemoCrash")
    Thread.sleep(5000)
    system.terminate()
  }

  /*
  * Exercise: how do we specify different supervisor strategies for different exceptions?
  * */

  val differentStrategies = Behaviors.supervise(
    Behaviors.supervise(FussyWordCounter())
    .onFailure[NullPointerException](SupervisorStrategy.resume)
  ).onFailure[IndexOutOfBoundsException](SupervisorStrategy.restart)

  def main(args: Array[String]): Unit = {
    demoSupervisionWithRestart()
  }
}

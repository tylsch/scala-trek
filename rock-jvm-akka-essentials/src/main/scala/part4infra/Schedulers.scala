package part4infra

import akka.actor.Cancellable
import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors

import scala.concurrent.duration.DurationInt

object Schedulers {

  object LoggerActor {
    def apply(): Behavior[String] = Behaviors.receive { (ctx, msg) =>
      ctx.log.info(s"[${ctx.self.path}] Received: $msg")
      Behaviors.same
    }
  }

  def demoScheduler(): Unit = {
    val userGuardian = Behaviors.setup[String] { ctx =>
      val loggerActor = ctx.spawn(LoggerActor(), "loggerActor")

      ctx.log.info("[system] System Starting")
      ctx.scheduleOnce(1.second, loggerActor, "reminder")

      Behaviors.empty
    }

    val system = ActorSystem(userGuardian, "DemoScheduler")
    import system.executionContext
    system.scheduler.scheduleOnce(2.second, () => system.terminate())
  }

  object ResettingTimeoutActor {
    def apply(): Behavior[String] = Behaviors.receive { (ctx, msg) =>
      resettingTimeoutActor(ctx.scheduleOnce(1.second, ctx.self, "timeout"))
    }
    def resettingTimeoutActor(schedule: Cancellable): Behavior[String] = Behaviors.receive { (ctx, msg) =>
      msg match {
        case "timeout" =>
          ctx.log.info("Stopping!")
          Behaviors.stopped
        case _ =>
          ctx.log.info(s"Received $msg")
          schedule.cancel()
          resettingTimeoutActor(ctx.scheduleOnce(1.second, ctx.self, "timeout"))
      }
    }
  }

  // timeout pattern
  def demoActorWithTimeout(): Unit = {
    import utils._
    val system = ActorSystem(ResettingTimeoutActor(), "TimeoutDemo").withFiniteLifespan(4.seconds)
    system ! "trigger"
    Thread.sleep(500)
    system ! "reset"
    Thread.sleep(500)
    system ! "still here"
  }

  def main(args: Array[String]): Unit = {
    demoActorWithTimeout()
  }
}

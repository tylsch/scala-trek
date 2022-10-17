import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors

import scala.concurrent.duration.{FiniteDuration, durationToPair}

package object utils {
  object LoggerActor {
    def apply[A](): Behavior[A] = Behaviors.receive { (ctx, msg) =>
      ctx.log.info(s"[${ctx.self.path}] Received: $msg")
      Behaviors.same
    }
  }

  implicit class ActorSystemEnhancements[A](system: ActorSystem[A]) {
    def withFiniteLifespan(duration: FiniteDuration): ActorSystem[A] = {
      import system.executionContext
      system.scheduler.scheduleOnce(duration, () => system.terminate())
      system
    }
  }
}

package part2actors

import akka.actor.typed.{ActorSystem, Behavior, PostStop}
import akka.actor.typed.scaladsl.Behaviors

object StoppingActors {

  object SensitiveActor {
    def apply(): Behavior[String] = Behaviors.receive[String] { (ctx, msg) =>
      ctx.log.info(s"Received : $msg")
      if (msg == "you're ugly")
        Behaviors.stopped // optionally pass a () => Unit to clear up resources after the actor is stopped
      else
        Behaviors.same
    }.receiveSignal {
      case (ctx, PostStop) =>
        ctx.log.info("I'm stopped now, not receiving other messages")
        Behaviors.same // not used anymore in case of stopping
    }
  }

  def main(args: Array[String]): Unit = {
    val userGuardianBehavior: Behavior[Unit] = Behaviors.setup { context =>
      val sensitiveActor = context.spawn(SensitiveActor(), "sensitiveActor")

      sensitiveActor ! "Hi"
      sensitiveActor ! "How are you?"
      sensitiveActor ! "you're ugly"
      sensitiveActor ! "sorry about that"
      // user guardian usually has no behavior of its own
      Behaviors.empty
    }

    val system = ActorSystem(userGuardianBehavior, "DemoParentChild")
    Thread.sleep(1000)
    system.terminate()
  }
}

package part4infra

import akka.actor.typed.{ActorSystem, Behavior, DispatcherSelector}
import akka.actor.typed.scaladsl.Behaviors
import com.typesafe.config.ConfigFactory
import utils._

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.Random

object DispatchersDemo {

  // Dispatchers are in charge of delivering and handling messages within an actor system

  def demoDispatcherConfig(): Unit = {
    val userGuardian = Behaviors.setup[Unit] { ctx =>
      val childActorDispatcherDefault = ctx.spawn(LoggerActor[String](), "childDefault", DispatcherSelector.default())
      val childActorBlocking = ctx.spawn(LoggerActor[String](), "childBlocking", DispatcherSelector.blocking())
      val childActorInherit = ctx.spawn(LoggerActor[String](), "childInherit", DispatcherSelector.sameAsParent())
      val childActorConfig = ctx.spawn(LoggerActor[String](), "childConfig", DispatcherSelector.fromConfig("my-dispatcher"))

      val actors = (1 to 10).map(i => ctx.spawn(LoggerActor[String](), s"child$i", DispatcherSelector.fromConfig("my-dispatcher")))

      val r = new Random()
      (1 to 1000).foreach(i => actors(r.nextInt(10)) ! s"task$i")
      Behaviors.empty
    }

    ActorSystem(userGuardian, "DemoDispatchers").withFiniteLifespan(2.seconds)
  }

  object DbActor {
    def apply(): Behavior[String] = Behaviors.receive { (ctx, msg) =>
      import ctx.executionContext // same as system.executioncontext, SAME AS THE SYSTEM'S DISPATCHER

      Future {
        Thread.sleep(1000)
        println(s"Query successful: $msg")
      }

      Behaviors.same
    }
  }

  def demoLockingCalls(): Unit = {
    val userGuardian = Behaviors.setup[Unit] { ctx =>
      val loggerActor = ctx.spawn(LoggerActor[String](), "logger")
      val dbActor = ctx.spawn(DbActor(), "db", DispatcherSelector.fromConfig("dedicated-blocking-dispatcher"))

      (1 to 100).foreach { i =>
        val message = s"query $i"
        dbActor ! message
        loggerActor ! message
      }

      Behaviors.same
    }

    ActorSystem(userGuardian, "DemoBlockingCalls", ConfigFactory.load().getConfig("dispatchers-demo"))
  }

  def main(args: Array[String]): Unit = {
    demoLockingCalls()
  }
}

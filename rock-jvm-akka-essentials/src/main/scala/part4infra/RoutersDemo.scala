package part4infra

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{Behaviors, GroupRouter, Routers}
import utils._

import scala.concurrent.duration.DurationInt

object RoutersDemo {

  def demoPoolRouter(): Unit = {
    val workerBehavior = LoggerActor[String]()
    val poolRouter = Routers.pool(5)(workerBehavior)
      .withBroadcastPredicate(_.length > 11)

    val userGuardian = Behaviors.setup[String] { ctx =>
      val poolActor = ctx.spawn(poolRouter, "pool")

      (1 to 10).foreach(i => poolActor ! s"work task $i")
      Behaviors.empty
    }

    ActorSystem(userGuardian, "DemoPoolRouter").withFiniteLifespan(2.seconds)
  }

  def demoGroupRouter(): Unit = {
    val serviceKey = ServiceKey[String]("logWorker")
    // service keys are used by a core akka module for discovering actors an fetching their Refs

    val userGuardian = Behaviors.setup[String] { ctx =>
      // in real life the workers may be created elsewhere in your code
      val workers = (1 to 5).map(i => ctx.spawn(LoggerActor[String](), s"worker$i"))
      // register workers with service key
      workers.foreach(worker => ctx.system.receptionist ! Receptionist.register(serviceKey, worker))

      val group: GroupRouter[String] = Routers.group(serviceKey)
        .withRoundRobinRouting()// random by default

      val router = ctx.spawn(group, "workerGroup")

      (1 to 10).foreach(i => router ! s"work task $i")

      // add new workers later
      Thread.sleep(1000)
      val extraWorker = ctx.spawn(LoggerActor[String](), "extraWorker")
      ctx.system.receptionist ! Receptionist.register(serviceKey, extraWorker)
      (1 to 10).foreach(i => router ! s"work task $i")

      /*
      * Removing workers:
      * - send the receptionist a Receptionist.deregister(serviceKey, worker, someActorToReceiveConfirmation)
      * - receive Receptionist.deregister is someActorToReceiveConfirmation, best practice, someActorToReceiveConfirmation == worker
      * -- in this time, there is a risk that the router might still use the worker as the router
      * - safe to stop the worker
      * */

      Behaviors.empty
    }

    ActorSystem(userGuardian, "DemoGroupRouter").withFiniteLifespan(2.seconds)
  }

  def main(args: Array[String]): Unit = {
    demoGroupRouter()
  }
}

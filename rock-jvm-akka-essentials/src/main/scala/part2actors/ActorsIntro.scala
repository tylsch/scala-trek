package part2actors

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors

object ActorsIntro {
  // part 1: behavior
  val simpleActorBehavior: Behavior[String] = Behaviors.receiveMessage{ (message: String) =>
    println(s"[simple actor] I have received: $message")

    // new behavior for the next message
    Behaviors.same
  }

  def demoSimpleActor(): Unit = {
    // part 2: instantiate
    val actorSystem = ActorSystem(SimpleActorV2(), "FirstActorSystem")

    // part 3: communicate
    actorSystem ! "I am learning Akka"  // asynchronously send a message

    // part 4: gracefully shutdown
    Thread.sleep(1000)
    actorSystem.terminate()
  }

  // "refactor"
  object SimpleActor {
    def apply(): Behavior[String] = Behaviors.receiveMessage { (message: String) =>
      println(s"[simple actor] I have received: $message")

      // new behavior for the next message
      Behaviors.same
    }
  }

  object SimpleActorV2 {
    def apply(): Behavior[String] = Behaviors.receive { (context, message) =>
      // context is a data structure that has access to a variety of APIs
      // simple example: logging
      context.log.info(s"[simple actor] I have received: $message")
      Behaviors.same
    }
  }

  object SimpleActorV3 {
    def apply(): Behavior[String] = Behaviors.setup { context =>
      // Actor "private" data and methods, behaviors
      // Your code here

      // Behavior used for the FIRST message
      Behaviors.receiveMessage { message =>
        context.log.info(s"[simple actor] I have received: $message")
        Behaviors.same
      }
    }
  }


  def main(args: Array[String]): Unit = {
    demoSimpleActor()
  }
}

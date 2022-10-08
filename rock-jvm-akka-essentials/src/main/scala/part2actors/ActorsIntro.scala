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
    val actorSystem = ActorSystem(Person(), "FirstActorSystem")

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

  /*
  * Exercises
  * 1. Define two "persons" actor behaviors, which receive Strings:
  * - "happy", which logs your message "I've received $message, That's great!"
  * - "sad",
  *
  * 2. Change the actor behavior:
  * - the happy behavior will turn to sad() if it receives "Akka is bad"
  * - the sad behavior will turn to happy() if it receives "Akka is awesome!"
  *
  * 3. Inspect my code and try to make it better
  * */

  object Person {
    def happy(): Behavior[String] = Behaviors.receive { (context, message) =>
      message match {
        case "Akka is bad." =>
          context.log.info("Don't you say anything bad about Akka")
          sad()
        case _ =>
          context.log.info(s"I've received '$message', That's great!")
          Behaviors.same
      }
    }

    def sad(): Behavior[String] = Behaviors.receive { (context, message) =>
      message match {
        case "Akka is awesome!" =>
          context.log.info("Happy now!")
          happy()
        case _ =>
          context.log.info(s"I've received '$message', That's sucks!")
          Behaviors.same
      }
    }

    def apply(): Behavior[String] = happy()
  }

  def testPerson() = {
    val person = ActorSystem(Person(), "PersonTest")

    person ! "I love the color blue"
    person ! "Akka is bad."
    person ! "I also love the color red"
    person ! "Akka is awesome!"
    person ! "I love Akka."

    Thread.sleep(1000)
    person.terminate()
  }

  object WeirdActor {
    // want to receive message of type Int and String
    def apply(): Behavior[Any] = Behaviors.receive { (context, message) =>
      message match {
        case number: Int =>
          context.log.info(s"I've received an int: $number")
          Behaviors.same
        case string: String =>
          context.log.info(s"I've received an String: $string")
          Behaviors.same
      }
    }
  }

  // solution: add wrapper types & type hierarchy (case classes/object)
  object BetterActor {
    trait Message
    case class IntMessage(number: Int) extends Message
    case class StringMessage(string: String) extends Message

    def apply(): Behavior[Message] = Behaviors.receive { (context, message) =>
      message match {
        case IntMessage(number) =>
          context.log.info(s"I've received an int: $number")
          Behaviors.same
        case StringMessage(string) =>
          context.log.info(s"I've received an String: $string")
          Behaviors.same
      }
    }
  }

  def demoWeirdActor(): Unit = {
    import BetterActor._
    val weirdActor = ActorSystem(BetterActor(), "WeirdActorDemo")
    weirdActor ! IntMessage(43)
    weirdActor ! StringMessage("Akka")
//    weirdActor ! '\t'

    Thread.sleep(1000)
    weirdActor.terminate()
  }


  def main(args: Array[String]): Unit = {
    demoWeirdActor()
  }
}

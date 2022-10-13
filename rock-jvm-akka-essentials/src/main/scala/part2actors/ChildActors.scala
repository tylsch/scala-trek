package part2actors

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors

object ChildActors {

  /*
  * actors can create other actors (child): parent ~> child ~> grandChild ~> ...
  * actor hierarchy = tree-like structure
  * root of the hierarchy = "guardian" actor (created with the ActorSystem)
  * actors can be identified via a path: /user/parent/child/grandchild/...
  * ActorSystem creates
  * - the top-level (root) guardian
  *   - system guardian (for Akka internal messages)
  *   - user guardian (for our custom actors)
  *  ALL OUR ACTORS OUR UNDER USER GUARDIAN ACTOR
  * */
  object Parent {
    trait Command
    case class CreateChild(name: String) extends Command
    case class TellChild(msg: String) extends Command
    case object StopChild extends Command

    def apply(): Behavior[Command] = idle()
    def idle(): Behavior[Command] = Behaviors.receive { (context, message) =>
      message match {
        case CreateChild(name) =>
          context.log.info(s"[parent] Creating child actor with name $name")
          // creating a child actor REFERENCE: (used to send messages to this child)
          val childRef: ActorRef[String] = context.spawn(Child(), name)
          active(childRef)
      }
    }

    def active(childRef: ActorRef[String]): Behavior[Command] = Behaviors.receive { (context, message) =>
      message match {
        case TellChild(msg) =>
          context.log.info(s"[parent] Sending message $msg to child")
          childRef ! msg // <- send a message to another actor
          Behaviors.same
        case StopChild =>
          context.log.info(s"[parent] stopping child")
          context.stop(childRef) // only works with child actors
          idle()
        case _ =>
          context.log.info("[parent] command not supported")
          Behaviors.same
      }
    }
  }

  object Child {
    def apply(): Behavior[String] = Behaviors.receive { (context, message) =>
      context.log.info(s"[${context.self.path.name}] Received $message")
      Behaviors.same
    }
  }

  def demoParentChild(): Unit = {
    import Parent._
    val userGuardianBehavior: Behavior[Unit] = Behaviors.setup { context =>
      val parentActor = context.spawn(Parent(), "parent")
      parentActor ! CreateChild("child")
      parentActor ! TellChild("hey kid, you there?")
      parentActor ! StopChild
      parentActor ! CreateChild("child2")
      parentActor ! TellChild("hey kid 2, you there?")
      // user guardian usually has no behavior of its own
      Behaviors.empty
    }

    val system = ActorSystem(userGuardianBehavior, "DemoParentChild")
    Thread.sleep(1000)
    system.terminate()
  }

  object ParentV2 {
    trait Command
    case class CreateChild(name: String) extends Command
    case class TellChild(name: String, msg: String) extends Command
    case class StopChild(name: String) extends Command
    def apply(): Behavior[Command] = active(Map())

    def active(children: Map[String, ActorRef[String]]): Behavior[Command] = Behaviors.receive { (context, message) =>
      message match {
        case CreateChild(name) =>
          context.log.info(s"[parent] Creating child actor with name $name")
          // creating a child actor REFERENCE: (used to send messages to this child)
          val childRef: ActorRef[String] = context.spawn(Child(), name)
          active(children + (name -> childRef))
        case TellChild(name, msg) =>
          val childOption = children.get(name)
          childOption.fold(context.log.info(s"[parent] child '$name' could not be found"))(child => child ! msg)
          Behaviors.same
        case StopChild(name) =>
          val childOption = children.get(name)
          childOption.fold(context.log.info(s"[parent] child '$name' could not be found"))(context.stop)
          active(children - name)
        case _ =>
          context.log.info("[parent] command not supported")
          Behaviors.same
      }
    }
  }

  def demoParentChildV2(): Unit = {
    import ParentV2._
    val userGuardianBehavior: Behavior[Unit] = Behaviors.setup { context =>
      val parentActor = context.spawn(ParentV2(), "parent")
      parentActor ! CreateChild("Alice")
      parentActor ! CreateChild("Bob")
      parentActor ! TellChild("Alice", "living next door to you")
      parentActor ! TellChild("Daniel", "I hope your Akka skills are good")
      parentActor ! StopChild("Alice")
      parentActor ! TellChild("Alice", "Hey Alice, you still there")
      // user guardian usually has no behavior of its own
      Behaviors.empty
    }

    val system = ActorSystem(userGuardianBehavior, "DemoParentChild")
    Thread.sleep(10000)
    system.terminate()
  }

  def main(args: Array[String]): Unit = {
    demoParentChildV2()
  }
}

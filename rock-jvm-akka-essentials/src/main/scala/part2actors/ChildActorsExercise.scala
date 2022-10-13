package part2actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

object ChildActorsExercise {
  /*
  * Exercise: distributed word counting example
  *   requester ----- (computational task) -----> WCM ----- (computational task) ------> one child of type WCW
  *   requester ----- (computational task) <----- WCM ----- (computational task) <------
  *
  * Scheme for scheduling tasks to children: round robin
  * [1-10]
  * task 1 - child 1
  * ...
  * task n - child n
  * */

  trait MasterProtocol
  trait WorkerProtocol
  trait UserProtocol
  // Master Messages
  case class Initialize(nChildren: Int) extends MasterProtocol
  case class WordCountTask(text: String, replyTo: ActorRef[UserProtocol]) extends MasterProtocol
  case class WordCountReply(id: Int, count: Int) extends MasterProtocol
  // Worker Messages
  case class WorkerTask(id: Int, text: String) extends WorkerProtocol
  // requester (user) messages
  case class Reply(count: Int) extends UserProtocol

  object WordCounterMaster {
    def apply(): Behavior[MasterProtocol] = Behaviors.receive { (ctx, msg) =>
      msg match {
        case Initialize(nChildren) =>
          ctx.log.info(s"[master] initializing with $nChildren children")
          val childRefs = for {
            i <- 1 to nChildren
          } yield ctx.spawn(WordCounterWorker(ctx.self), s"worker$i")
          active(childRefs, 0, 0, Map())
        case _ =>
          ctx.log.info(s"[master] Command not supported while idle")
          Behaviors.same
      }
    }

    def active(
                childRefs: Seq[ActorRef[WorkerProtocol]],
                currentChildIndex: Int,
                currentTaskId: Int,
                requestMap: Map[Int, ActorRef[UserProtocol]]): Behavior[MasterProtocol] = Behaviors.receive { (ctx, msg) =>
      msg match {
        case WordCountTask(text, replyTo) =>
          ctx.log.info(s"[master] I've received $text - I will send it to the child $currentChildIndex")
          val task = WorkerTask(currentTaskId, text)
          val childRef = childRefs(currentChildIndex)

          childRef ! task
          val nextChildIndex = (currentChildIndex + 1) % childRefs.length
          val nextTaskId = currentTaskId + 1
          val newRequestMap = requestMap + (currentTaskId -> replyTo)

          active(childRefs, nextChildIndex, nextTaskId, newRequestMap)
        case WordCountReply(id, count) =>
          ctx.log.info(s"[master] I've received a reply for task id $id, with $count")
          val sender = requestMap(id)
          sender ! Reply(count)
          active(childRefs, currentChildIndex, currentTaskId, requestMap - id)
        case _ =>
          ctx.log.info(s"[master] Command not supported while active")
          Behaviors.same
      }
    }
  }
  object WordCounterWorker {
    def apply(masterRef: ActorRef[MasterProtocol]): Behavior[WorkerProtocol] = Behaviors.receive { (ctx, msg) =>
      msg match {
        case WorkerTask(id, text) =>
          ctx.log.info(s"[${ctx.self.path} I've received task $id with '$text'")
          val result = text.split(" ").length
          masterRef ! WordCountReply(id, result)
          Behaviors.same
        case _ =>
          ctx.log.info(s"[${ctx.self.path} Command not supported")
          Behaviors.same
      }
    }
  }
  object Aggregator {
    def apply(): Behavior[UserProtocol] = active()
    def active(totalWords: Int = 0): Behavior[UserProtocol] = Behaviors.receive { (context, messages) =>
      messages match {
        case Reply(count) =>
          context.log.info(s"[aggregator] I've received $count, total is ${totalWords + count}")
          active(totalWords + count)
      }
    }
  }

  def testWordCounter(): Unit = {
    val userGuardianBehavior: Behavior[Unit] = Behaviors.setup { context =>
      val aggregator = context.spawn(Aggregator(), "aggregator")
      val wcm = context.spawn(WordCounterMaster(), "master")

      wcm ! Initialize(3)
      wcm ! WordCountTask("I love Akka", aggregator)
      wcm ! WordCountTask("Scala is super dope", aggregator)
      wcm ! WordCountTask("yes it is", aggregator)
      wcm ! WordCountTask("Testing round robin scheduling", aggregator)
      Behaviors.empty
    }

    val system = ActorSystem(userGuardianBehavior, "WordCounting")
    Thread.sleep(5000)
    system.terminate()
  }

  def main(args: Array[String]): Unit = {
    testWordCounter()
  }
}

package part3testing

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import org.scalatest.wordspec.AnyWordSpecLike
import part3testing.UsingProbesSpec._

class UsingProbesSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  "A master actor" should {
    val master = testKit.spawn(Master(), "master")
    val workerProbe = testKit.createTestProbe[WorkerTask]()
    val externalProbe = testKit.createTestProbe[ExternalProtocol]()

    "register a worker" in {
      master ! Register(workerProbe.ref, externalProbe.ref)
      externalProbe.expectMessage(RegisterAck)
    }
    "send a task to the worker actor" in {
      master ! Register(workerProbe.ref, externalProbe.ref)
      externalProbe.expectMessage(RegisterAck)
      master ! Work("I love Akka", externalProbe.ref)

      workerProbe.expectMessage(WorkerTask("I love Akka", master.ref, externalProbe.ref))
      master ! WorkCompleted(3, externalProbe.ref)
      externalProbe.expectMessage(Report(3))
    }
    "aggregate data correctly" in {
      val mockedWorkerBehavior = Behaviors.receiveMessage[WorkerTask] {
        case WorkerTask(_, master, sender) => master ! WorkCompleted(3, sender)
          Behaviors.same
      }
      val mockedWorker = testKit.spawn(Behaviors.monitor(workerProbe.ref, mockedWorkerBehavior))

      master ! Register(mockedWorker, externalProbe.ref)
      externalProbe.expectMessage(RegisterAck)
      master ! Work("I love Akka", externalProbe.ref)
      master ! Work("I love Akka", externalProbe.ref)

      externalProbe.expectMessage(Report(3))
      externalProbe.expectMessage(Report(6))
    }
  }
}
object UsingProbesSpec {
  /*
  * requester -> master -> worker
  *           <-        <-
  * */
  trait MasterProtocol
  case class Work(text: String, replyTo: ActorRef[ExternalProtocol]) extends MasterProtocol
  case class WorkCompleted(count: Int, sender: ActorRef[ExternalProtocol]) extends MasterProtocol
  case class Register(workerRef: ActorRef[WorkerTask], replyTo: ActorRef[ExternalProtocol]) extends MasterProtocol

  case class WorkerTask(text: String, master: ActorRef[MasterProtocol], sender: ActorRef[ExternalProtocol])

  trait ExternalProtocol
  case class Report(totalCount: Int) extends ExternalProtocol
  case object RegisterAck extends ExternalProtocol

  object Master {
    def apply(): Behavior[MasterProtocol] = Behaviors.receiveMessage {
      case Register(workerRef, replyTo) =>
        replyTo ! RegisterAck
        active(workerRef)
      case _ =>
        Behaviors.same
    }

    def active(workerRef: ActorRef[WorkerTask], totalCount: Int = 0): Behavior[MasterProtocol] =
      Behaviors.receive { (ctx, msg) =>
        msg match {
          case Work(text, replyTo) =>
            workerRef ! WorkerTask(text, ctx.self, replyTo)
            Behaviors.same
          case WorkCompleted(count, sender) =>
            val newTotalCount = totalCount + count
            sender ! Report(newTotalCount)
            active(workerRef, newTotalCount)
        }
    }
  }
}

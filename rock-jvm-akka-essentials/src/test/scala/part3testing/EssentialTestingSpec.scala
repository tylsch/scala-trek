package part3testing

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import org.scalatest.wordspec.AnyWordSpecLike
import part3testing.EssentialTestingSpec._

import scala.concurrent.duration.DurationInt

class EssentialTestingSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  "A simple actor" should {
    "send back a duplicated message" in {
      val simpleActor = testKit.spawn(SimpleActor(), "simpleActor") // actor under test
      val probe = testKit.createTestProbe[SimpleProtocol]() // "inspector

      simpleActor ! SimpleMessage("Akka", probe.ref)

      // assertions
      probe.expectMessage(SimpleReply("AkkaAkka"))
    }
  }

  "A simple actor with a separate test suite" should {
    val simpleActor = testKit.spawn(SimpleActor(), "simpleActor") // actor under test
    val probe = testKit.createTestProbe[SimpleProtocol]() // "inspector

    "uppercase a string" in {
      simpleActor ! UppercaseString("Akka", probe.ref)
      val receivedMessage = probe.expectMessageType[SimpleReply]
      // other assertions
      assert(receivedMessage.contents == receivedMessage.contents.toUpperCase()) // Scala standard assertion
      receivedMessage.contents should be("AKKA") // ScalaTest library assertion
    }

    "reply with favorite tech as multiple messages" in {
      simpleActor ! FavoriteTech(probe.ref)

      val replies: Seq[SimpleProtocol] = probe.receiveMessages(2, 1.second)
      val repliesContents: Seq[String] = replies.collect {
        case SimpleReply(contents) => contents
      }

      repliesContents should contain allOf("Scala", "Akka")
    }
  }

  "A black hole actor" should {
    "not reply back" in {
      val blackHole = testKit.spawn(BlackHole(), "blackHole")
      val probe = testKit.createTestProbe[SimpleProtocol]() // "inspector

      blackHole ! SimpleMessage("I love Akka", probe.ref)
      blackHole ! SimpleMessage("Hey can you hear me?", probe.ref)
      blackHole ! SimpleMessage("I love Akka", probe.ref)

      probe.expectNoMessage(1.second)
    }
  }
}
object EssentialTestingSpec {
  trait SimpleProtocol
  case class SimpleMessage(message: String, sender: ActorRef[SimpleProtocol]) extends SimpleProtocol
  case class UppercaseString(message: String, replyTo: ActorRef[SimpleProtocol]) extends SimpleProtocol
  case class FavoriteTech(replyTo: ActorRef[SimpleProtocol]) extends SimpleProtocol
  case class SimpleReply(contents: String) extends SimpleProtocol

  object SimpleActor {
    def apply(): Behavior[SimpleProtocol] = Behaviors.receiveMessage {
      case SimpleMessage(message, sender) =>
        sender ! SimpleReply(message + message)
        Behaviors.same
      case UppercaseString(message, replyTo) =>
        replyTo ! SimpleReply(message.toUpperCase)
        Behaviors.same
      case FavoriteTech(replyTo) =>
        replyTo ! SimpleReply("Scala")
        replyTo ! SimpleReply("Akka")
        Behaviors.same
    }
  }

  object BlackHole {
    def apply(): Behavior[SimpleProtocol] = Behaviors.ignore
  }
}

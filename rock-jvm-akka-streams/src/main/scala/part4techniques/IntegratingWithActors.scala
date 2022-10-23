package part4techniques

import akka.Done
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.stream.{CompletionStrategy, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.Timeout

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object IntegratingWithActors extends App {
  implicit val system = ActorSystem("IntegratingWithActors")

  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case s: String =>
        log.info(s"Just received a string: $s")
        sender() ! s"$s$s"
      case n: Int =>
        log.info(s"Just received a number: $n")
        sender() ! (2 * n)
      case _ =>
    }
  }

  val simpleActor = system.actorOf(Props[SimpleActor], "simpleActor")

  val numbersSource = Source(1 to 10)

  implicit val timeout = Timeout(2 seconds)
  val actorBasedFlow = Flow[Int].ask[Int](parallelism = 4)(simpleActor)

//  numbersSource.via(actorBasedFlow).to(Sink.ignore).run()
//  numbersSource.ask[Int](parallelism = 4)(simpleActor).to(Sink.ignore).run() // same as above

  /*
  * Actor as a source
  * */

  val actorPoweredSource: Source[Any, ActorRef] = Source.actorRef(
    completionMatcher = {
      case Done =>
        // complete stream immediately if we send it Done
        CompletionStrategy.immediately
    },
    // never fail the stream because of a message
    failureMatcher = PartialFunction.empty,
    bufferSize = 100,
    overflowStrategy = OverflowStrategy.dropHead)
//  val actorRef: ActorRef = actorPoweredSource.to(Sink.foreach(println)).run()
//
//  actorRef ! "hello"
//  actorRef ! "hello"
//  // The stream completes successfully with the following message
//  actorRef ! Done
//  actorRef ! "hello"

  /*
  * Actor as a destination/sink
  * - an init message
  * - an ack message to confirm the reception
  * - a complete message
  * - a function to generate a message in case the stream throws an exception
  * */

  case object StreamInit
  case object StreamAck
  case object StreamComplete
  case class StreamFail(ex: Throwable)

  class DestinationActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case StreamInit =>
        log.info("Stream Started")
        sender() ! StreamAck
      case StreamComplete =>
        log.info("Stream Complete")
        context.stop(self)
      case StreamFail(ex) =>
        log.warning(s"Stream failed: $ex")
      case message =>
        log.info(s"Message $message has come to its final resting point")
        sender() ! StreamAck
    }
  }
  val destinationActor = system.actorOf(Props[DestinationActor], "destinationActor")

  val actorPoweredSink = Sink.actorRefWithBackpressure(
    destinationActor,
    onInitMessage = StreamInit,
    onCompleteMessage = StreamComplete,
    onFailureMessage = throwable => StreamFail(throwable),
    ackMessage = StreamAck
  )
  Source(1 to 10).to(actorPoweredSink).run()
}

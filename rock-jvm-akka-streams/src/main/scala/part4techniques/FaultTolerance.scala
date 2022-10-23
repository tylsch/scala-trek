package part4techniques

import akka.actor.ActorSystem
import akka.stream.Supervision.{Resume, Stop}
import akka.stream.{ActorAttributes, RestartSettings}
import akka.stream.scaladsl.{RestartSource, Sink, Source}

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.Random

object FaultTolerance extends App {
  implicit val system = ActorSystem("FaultTolerance")

  // 1 - Logging
  val faultySource = Source(1 to 10).map(e => if (e == 6) throw new RuntimeException else e)
//  faultySource.log("trackingElements").to(Sink.ignore).run()

  // 2 - gracefully terminating a stream
  faultySource.recover {
    case _: RuntimeException => Int.MinValue
  }.log("gracefulSource")
    .to(Sink.ignore)
//    .run()

  // 3 - recover with another stream
  faultySource.recoverWithRetries(3, {
    case _: RuntimeException => Source(90 to 99)
  }).log("recoverWithRetries")
    .to(Sink.ignore)
//    .run()

  // 4 - backoff supervision
  val restartSource = RestartSource.onFailuresWithBackoff(RestartSettings(1 second, 30 seconds, 0.2))(
    () => {
      val randomNumber = new Random().nextInt(20)
      Source(1 to 10).map(e => if (e == randomNumber) throw new RuntimeException else e)
    }
  )

//  restartSource
//    .log("restartBackoff")
//    .to(Sink.ignore)
//    .run()

  // 5 - supervision strategy
  val numbers = Source(1 to 20).map(n => if (n == 13) throw new RuntimeException("back luck") else n).log("supervision")
  val supervisedNumbers = numbers.withAttributes(ActorAttributes.supervisionStrategy {
    // Resume, Stop, Restart
    /*
    * Resume = skips the faulty element
    * Stop = stops stream
    * Restart = resume + clears internal state of component
    * */
    case _: RuntimeException => Resume
    case _ => Stop
  })

  supervisedNumbers.to(Sink.ignore).run()
}

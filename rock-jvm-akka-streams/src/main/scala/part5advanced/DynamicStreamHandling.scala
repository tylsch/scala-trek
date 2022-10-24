package part5advanced

import akka.actor.ActorSystem
import akka.stream.KillSwitches
import akka.stream.scaladsl.{BroadcastHub, Keep, MergeHub, Sink, Source}

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object DynamicStreamHandling extends App {
  implicit val system = ActorSystem("DynamicStreamHandling")
  import system.dispatcher

  // 1 -  Kill Switch
  val killSwitchFlow = KillSwitches.single[Int]
  val counter = Source(LazyList.from(1)).throttle(1, 1 second).log("counter")
  val sink = Sink.ignore

//  val killSwitch = counter
//    .viaMat(killSwitchFlow)(Keep.right)
//    .to(sink)
//    .run()
//
//  system.scheduler.scheduleOnce(3 seconds) {
//    killSwitch.shutdown()
//  }

  val anotherCounter = Source(LazyList.from(1)).throttle(2, 1 second).log("anotherCounter")
  val sharedKillSwitches = KillSwitches.shared("oneButtonToRuleThemAll")

  counter.via(sharedKillSwitches.flow).runWith(Sink.ignore)
  anotherCounter.via(sharedKillSwitches.flow).runWith(Sink.ignore)

  system.scheduler.scheduleOnce(3 seconds) {
    sharedKillSwitches.shutdown()
  }

  // MergeHub
  val dynamicMerge = MergeHub.source[Int]
  val materializedSink = dynamicMerge.to(Sink.foreach[Int](println)).run()

  // use this sink any time we like
  Source(1 to 10).runWith(materializedSink)
  counter.runWith(materializedSink)

  // BroadcastHub
  val dynamicBroadcast = BroadcastHub.sink[Int]
  val materializedSource = Source(1 to 100).runWith(dynamicBroadcast)

  materializedSource.runWith(Sink.ignore)
  materializedSource.runWith(Sink.foreach[Int](println))

  val merge = MergeHub.source[String]
  val broadcast = BroadcastHub.sink[String]
  val (pubPort, subPort) = merge.toMat(broadcast)(Keep.both).run()

  subPort.runWith(Sink.foreach(e => println(s"I received:: $e")))
  subPort.map(_.length).runWith(Sink.foreach(n => println(s"I got a number:: $n")))

  Source(List("Akka", "is", "amazing")).runWith(pubPort)
  Source(List("I", "Love", "Scala")).runWith(pubPort)
  Source.single("STREEEMSSS").runWith(pubPort)

}

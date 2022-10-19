package part2primer

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, Source}

object OperatorFusion extends App {
  implicit val system: ActorSystem = ActorSystem("OperatorFusion")

  val simpleSource = Source(1 to 1000)
  val simpleFlow = Flow[Int].map(_ + 1)
  val simpleFlow2 = Flow[Int].map(_ * 10)
  val simpleSink = Sink.foreach[Int](println)

  // this runs on the SAME ACTOR
//  simpleSource.via(simpleFlow).via(simpleFlow2).to(simpleSink).run()
  // operator/component FUSION

  // complex flows:
  val complexFlow = Flow[Int].map { x =>
    Thread.sleep(1000)
    x + 1
  }
  val complexFlow2 = Flow[Int].map { x =>
    Thread.sleep(1000)
    x * 10
  }

//  simpleSource.via(complexFlow).via(complexFlow2).to(simpleSink).run()

  // async boundary
//  simpleSource.via(complexFlow).async
//    .via(complexFlow2).async
//    .to(simpleSink)
//    .run()

  // ordering guarantees
  Source(1 to 3)
    .map(e => { println(s"Flow A: $e"); e}).async
    .map(e => { println(s"Flow B: $e"); e}).async
    .map(e => { println(s"Flow C: $e"); e}).async
    .runWith(Sink.ignore)
}

package part2primer

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent.Future

object FirstPrinciples extends App {
  implicit val system = ActorSystem("FirstPrinciples")

  // source
  val source = Source(1 to 10)
  // sink
  val sink = Sink.foreach[Int](println)

  val graph = source.to(sink)
  graph.run()

  // flows transform elements
  val flow = Flow[Int].map(x => x + 1)
  val sourceWithFlow = source.via(flow)
  val flowWithSink = flow.to(sink)

//  sourceWithFlow.to(sink).run()
//  source.to(flowWithSink).run()
//  source.via(flow).to(sink).run()

  // nulls ARE NOT ALLOWED!!!, USE OPTIONS INSTEAD!!!!!
//  val illegalSource = Source.single[String](null)
//  illegalSource.to(Sink.foreach(println))

  // various kinds of sources
  val finiteSource = Source.single(1)
  val anotherFiniteSource = Source(List(1,2,3))
  val emptySource = Source.empty[Int]
  val inifiniteSource = Source(LazyList.from(1)) // Stream is deprecated, use LazyList

  import scala.concurrent.ExecutionContext.Implicits.global
  val futureSource = Source.future[Int](Future(42))

  // sinks
  val theMostBoringSink = Sink.ignore
  val foreachSink = Sink.foreach[String](println)
  val headSink = Sink.head[Int] // retrieves head and then closes the stream
  val foldSink = Sink.fold[Int, Int](0)((a,b) => a + b)

  // flows - usually maps to collection operators
  val mapFlow = Flow[Int].map(x => 2 * x)
  val takeFlow = Flow[Int].take(5)
  // drop, filter,
  // NO flatMap

  // source -> flow -> flow -> ... -> sink
  val doubleFlowGraph = source.via(mapFlow).via(takeFlow).to(sink)
  doubleFlowGraph.run()

  // syntactic sugars
  val mapSource = Source(1 to 10).map(_ * 2)
  // run streams directly
  mapSource.runForeach(println)

  // OPERATORS = components
  /*
  * Exercise:
  * - create a stream that takes the names of a person, then you will keep the first two names where length is > 5 chars
  * */

  val names = List("Alice", "Bob", "Charlie", "Martin", "AkkaStreams")
  val nameSource = Source(names)
  val filterFlow = Flow[String].filter(_.length > 5)
  val limitFlow = Flow[String].take(2)
  val nameSink = Sink.foreach[String](println)

  nameSource.via(filterFlow).via(limitFlow).to(nameSink).run()
  nameSource.filter(_.length > 5).take(2).runForeach(println)
}

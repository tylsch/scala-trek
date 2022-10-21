package part3Graphs

import akka.actor.ActorSystem
import akka.stream.{FlowShape, SinkShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, Sink, Source}

import scala.concurrent.Future
import scala.util.{Failure, Success}

object GraphMaterializedValues extends App {
  implicit val system = ActorSystem("GraphMaterializedValues")

  val wordSource = Source(List("Akka", "is", "awesome", "rock", "the", "jvm"))
  val printer = Sink.foreach[String](println)
  val counter = Sink.fold[Int, String](0)((count, _) => count + 1)

  /*
  * A composite component (sink)
  * - prints out all string which are lowercase
  * - COUNTS the strings that are short (< 5 chars)
  * */

  val complexWordSink = Sink.fromGraph(
    GraphDSL.createGraph(printer, counter)((_, counterMatValue) => counterMatValue) {
      implicit builder => (printerShape, counterShape) =>
        import GraphDSL.Implicits._

        val broadcast = builder.add(Broadcast[String](2))
        val lowercaseFilter = builder.add(Flow[String].filter(word => word == word.toLowerCase))
        val shortStringFilter = builder.add(Flow[String].filter(_.length < 5))

        broadcast ~> lowercaseFilter ~> printerShape
        broadcast ~> shortStringFilter ~> counterShape

        SinkShape(broadcast.in)
    }
  )

  import system.dispatcher
  val shortStringCountFuture = wordSource.toMat(complexWordSink)(Keep.right).run()
  shortStringCountFuture.onComplete {
    case Success(value) => println(s"The total number of short strings is $value")
    case Failure(exception) => println(s"The count of short strings failed: $exception")
  }

  /*
  * Exercise
  * */
  def enhanceFlow[A, B](flow: Flow[A, B, _]): Flow[A, B, Future[Int]] = {
    val counterSink = Sink.fold[Int, B](0)((count, _) => count + 1)
    Flow.fromGraph(
      GraphDSL.createGraph(counterSink) {
        implicit builder => counterSinkShape =>
          import GraphDSL.Implicits._

          val broadcast = builder.add(Broadcast[B](2))
          val originalFlowShape = builder.add(flow)

          originalFlowShape ~> broadcast ~> counterSinkShape

          FlowShape(originalFlowShape.in, broadcast.out(1))
      }
    )
  }
  val simpleSource = Source(1 to 42)
  val simpleFlow = Flow[Int].map(x => x)
  val simpleSink = Sink.ignore

  val enhancedFlowCountFuture = simpleSource.viaMat(enhanceFlow(simpleFlow))(Keep.right).toMat(simpleSink)(Keep.left).run()
  enhancedFlowCountFuture.onComplete {
    case Success(value) => println(s"$value went through the enhanced flow")
    case Failure(exception) => println(s"Failed: $exception")
  }

}

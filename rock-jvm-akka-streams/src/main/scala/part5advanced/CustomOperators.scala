package part5advanced

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{Attributes, Inlet, Outlet, SinkShape, SourceShape}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}

import scala.collection.mutable
import scala.util.Random

object CustomOperators extends App {
  implicit val system: ActorSystem = ActorSystem("CustomOperators")

  // 1 - a custom source which emits random numbers until canceled

  class RandomNumberGenerator(max: Int) extends GraphStage[SourceShape[Int]] {
    val outPort: Outlet[Int] = Outlet[Int]("randomGenerator")
    val random = new Random()

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
      // implement my logic here
      setHandler(outPort, new OutHandler {
        // when there is demand from downstream
        override def onPull(): Unit = {
          // emit a new element
          val nextNumber = random.nextInt(max)
          // push it out the outPort
          push(outPort, nextNumber)
        }
      })
    }
    override def shape: SourceShape[Int] = SourceShape(outPort)
  }

  val randomNumberGeneratorSource = Source.fromGraph(new RandomNumberGenerator(100))
  //randomNumberGeneratorSource.runWith(Sink.foreach(println))

  // 2 - a custom sink that prints elements in batches of a given size
  class Batcher(batchSize: Int) extends GraphStage[SinkShape[Int]] {
    val inPort: Inlet[Int] = Inlet[Int]("batcher")

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
      override def preStart(): Unit = {
        pull(inPort)
      }
      // mutable state
      val batch = new mutable.Queue[Int]

      setHandler(inPort, new InHandler {
        // when the upstream wants to send an element
        override def onPush(): Unit = {
          val nextElement = grab(inPort)
          batch.enqueue(nextElement)

          //  assume some complex computation
          Thread.sleep(100)
          if (batch.size >= batchSize) {
            println("New Batch: " + batch.dequeueAll(_ => true).mkString("[", ", ", "]"))
          }

          pull(inPort) // send demand upstream
        }

        override def onUpstreamFinish(): Unit = {
          if (batch.nonEmpty) {
            println("New Batch: " + batch.dequeueAll(_ => true).mkString("[", ", ", "]"))
            println("Stream finished")
          }
        }
      })
    }
    override def shape: SinkShape[Int] = SinkShape(inPort)
  }

  val batcherSink = Sink.fromGraph(new Batcher(10))
  randomNumberGeneratorSource.to(batcherSink).run()
}

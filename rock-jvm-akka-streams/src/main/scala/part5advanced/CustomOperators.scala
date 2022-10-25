package part5advanced

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet, SinkShape, SourceShape}
import akka.stream.stage.{GraphStage, GraphStageLogic, GraphStageWithMaterializedValue, InHandler, OutHandler}

import scala.collection.mutable
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Random, Success}

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
  //randomNumberGeneratorSource.to(batcherSink).run()

  class SimpleFilter[T](predicate: T => Boolean) extends GraphStage[FlowShape[T, T]] {
    val inPort = Inlet[T]("filterIn")
    val outPort = Outlet[T]("filterOut")

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
      setHandler(outPort, new OutHandler {
        override def onPull(): Unit = pull(inPort)
      })
      setHandler(inPort, new InHandler {
        override def onPush(): Unit = {
          try {
            val nextElement = grab(inPort)

            if (predicate(nextElement))
              push(outPort, nextElement)
            else
              pull(inPort)
          }
          catch {
            case e: Throwable => failStage(e)
          }
        }
      })
    }
    override def shape: FlowShape[T, T] = FlowShape(inPort, outPort)
  }

  val myFilter = Flow.fromGraph(new SimpleFilter[Int](_ > 50))
  //randomNumberGeneratorSource.via(myFilter).to(batcherSink).run()

  /*
  * Materialized Values in Graph Stages
  * */

  // 3 - a flow that counts the number of elements that goes through it
  class CounterFlow[T] extends GraphStageWithMaterializedValue[FlowShape[T, T], Future[Int]] {
    val inPort: Inlet[T] = Inlet[T]("counterIn")
    val outPort: Outlet[T] = Outlet[T]("counterOut")
    override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, Future[Int]) = {
      val promise = Promise[Int]
      val logic: GraphStageLogic = new GraphStageLogic(shape) {
        // setting mutable state
        var counter = 0

        setHandler(outPort, new OutHandler {
          override def onPull(): Unit = pull(inPort)

          override def onDownstreamFinish(cause: Throwable): Unit = {
            promise.success(counter)
            super.onDownstreamFinish(cause)
          }
        })
        setHandler(inPort, new InHandler {
          override def onPush(): Unit = {
            val nextElement = grab(inPort)
            counter += 1
            push(outPort, nextElement)
          }
          override def onUpstreamFinish(): Unit = {
            promise.success(counter)
            super.onUpstreamFinish()
          }
          override def onUpstreamFailure(ex: Throwable): Unit = {
            promise.failure(ex)
            super.onUpstreamFailure(ex)
          }
        })
      }

      (logic, promise.future)
    }
    override val shape: FlowShape[T, T] = FlowShape(inPort, outPort)
  }

  val counterFlow = Flow.fromGraph(new CounterFlow[Int])
  val countFuture = Source(1 to 10)
    //.map(x => if (x == 7) throw new RuntimeException else x)
    .viaMat(counterFlow)(Keep.right)
    //.to(Sink.foreach(x => if (x == 7) throw new RuntimeException else println(x)))
    .to(Sink.foreach[Int](println))
    .run()

  import system.dispatcher
  countFuture.onComplete {
    case Success(value) => println(s"The number of elements passed: $value")
    case Failure(exception) => println(s"Counting the elements failed: $exception")
  }
}

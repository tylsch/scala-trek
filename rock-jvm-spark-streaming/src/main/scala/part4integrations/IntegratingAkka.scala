package part4integrations

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Sink, Source}
import com.typesafe.config.ConfigFactory
import common.{Car, carsSchema}
import org.apache.spark.sql.{Dataset, SaveMode, SparkSession}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import part4integrations.IntegratingJDBC.spark

object IntegratingAkka {
  val spark: SparkSession = SparkSession.builder()
    .appName("Integrating Akka")
    .master("local[2]")
    .getOrCreate()

  spark.sparkContext.setLogLevel("ERROR")

  val ssc = new StreamingContext(sparkContext = spark.sparkContext, batchDuration = Seconds(1))

  // foreachBatch
  // receiving system is on another JVM

  import spark.implicits._

  def writeCarsToAkka(): Unit = {
    val carsDS = spark.readStream
      .schema(carsSchema)
      .json("src/main/resources/data/cars")
      .as[Car]

    carsDS.writeStream
      .foreachBatch { (batch: Dataset[Car], batchId: Long) =>
        batch.foreachPartition { cars: Iterator[Car] =>
          // this code is run by a single executor
          val system = ActorSystem(s"SourceSystem$batchId", ConfigFactory.load("akkaconfig/remoteActors"))
          val entryPoint = system.actorSelection("akka://ReceiverSystem@localhost:2552/user/entrypoint")

          cars.foreach(car => entryPoint ! car)
        }
      }
      .start()
      .awaitTermination()
  }

  def main(args: Array[String]): Unit = {
    writeCarsToAkka()
  }
}

object ReceiverSystem {

  class Destination extends Actor with ActorLogging {
    override def receive: Receive = {
      case m => log.info(m.toString)
    }
  }

  object EntryPoint {
    def props(destination: ActorRef): Props = Props(new EntryPoint(destination))
  }
  class EntryPoint(actorRef: ActorRef) extends Actor with ActorLogging {
    override def receive: Receive = {
      case m =>
        log.info(s"Received $m")
        actorRef ! m
    }
  }

  def main(args: Array[String]): Unit = {
    implicit val actorSystem: ActorSystem = ActorSystem("ReceiverSystem", ConfigFactory.load("akkaconfig/remoteActors").getConfig("remoteSystem"))
    implicit val actorMaterializer: ActorMaterializer = ActorMaterializer()

    val source = Source.actorRef[Car](bufferSize = 10, overflowStrategy = OverflowStrategy.dropHead)
    val sink = Sink.foreach[Car](println)
    val runnableGraph = source.to(sink)
    val destination = runnableGraph.run()

//    val destination = actorSystem.actorOf(Props[Destination], "destination")
    val entryPoint = actorSystem.actorOf(EntryPoint.props(destination), "entrypoint")
  }
}

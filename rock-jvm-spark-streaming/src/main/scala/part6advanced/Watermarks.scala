package part6advanced

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming.{StreamingQuery, Trigger}

import java.io.PrintStream
import java.net.{ServerSocket, Socket}
import java.sql.Timestamp
import scala.concurrent.duration._

object Watermarks {
  val spark: SparkSession = SparkSession.builder()
    .appName("Watermarks")
    .master("local[*]")
    .getOrCreate()

  spark.sparkContext.setLogLevel("ERROR")

  import spark.implicits._

  def debugQuery(query: StreamingQuery): Thread =  {
    new Thread(() => {
      (1 to 100).foreach { i =>
        Thread.sleep(1000)
        val queryEventTime =
          if (query.lastProgress == null) "[]"
          else query.lastProgress.eventTime.toString

        println(s"$i: $queryEventTime")
      }
    })
  }

  def testWatermarks(): Unit = {
    val dataDF = spark.readStream
      .format("socket")
      .option("host", "localhost")
      .option("port", 12345)
      .load()
      .as[String]
      .map { line =>
        val tokens = line.split(",")
        val timestamp = new Timestamp(tokens(0).toLong)
        val data = tokens(1)

        (timestamp, data)
      }
      .toDF("created", "color")

    val watermarkedDF = dataDF
      .withWatermark("created", "2 seconds") // adding a 2 second watermark
      .groupBy(window(col("created"), "2 seconds"), col("color"))
      .count()
      .selectExpr("window.*", "color", "count")

    /*
    * A 2 second watermark means
    * - a window will only be considered until the watermark surpasses the window end
    * - an element/record/row will be considered if AFTER the watermark
    * */

    val query = watermarkedDF
      .writeStream
      .format("console")
      .outputMode("append")
      .trigger(Trigger.ProcessingTime(2.seconds))
      .start()

    // useful skill for debug
    debugQuery(query)
    query.awaitTermination()
  }

  def main(args: Array[String]): Unit = {
    testWatermarks()
  }
}

object DataSender {
  val serverSocket = new ServerSocket(12345)
  val socket: Socket = serverSocket.accept() // blocking call
  val printer = new PrintStream(socket.getOutputStream)

  println("socket accepted")

  def example1() = {
    Thread.sleep(7000)
    printer.println("7000,blue")
    Thread.sleep(1000)
    printer.println("8000,green")
    Thread.sleep(4000)
    printer.println("14000,blue")
    Thread.sleep(1000)
    printer.println("9000,red")
    Thread.sleep(3000)
    printer.println("15000,red")
    printer.println("8000,blue")
    Thread.sleep(1000)
    printer.println("13000,green")
    Thread.sleep(500)
    printer.println("21000,green")
    Thread.sleep(3000)
    printer.println("4000,purple") // expect to drop
    Thread.sleep(2000)
    printer.println("17000,green")
  }

  def main(args: Array[String]): Unit = {
    example1()
  }
}

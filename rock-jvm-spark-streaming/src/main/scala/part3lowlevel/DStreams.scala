package part3lowlevel

import common.Stock
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.{Seconds, StreamingContext}

import java.io.File
import java.sql.Date
import java.text.SimpleDateFormat

object DStreams {
  val spark = SparkSession.builder()
    .appName("DStreams")
    .master("local[2]")
    .getOrCreate()

  /*
    * Spark Streaming Context = entry point to DStreams API
    * - needs the spark context
    * - a duration = batch interval
    * */

  val ssc = new StreamingContext(spark.sparkContext, Seconds(1))

  /*
  * - define input sources by creating DStreams
  * - define transformations on DStreams
  * - call an action on DStreams
  * - start computation with ssc.start()
  *   - no more computations can be added
  * - await termination, or stop the computation
  *   - you cannot restart a computation
  * */

  def readFromSocket(): Unit = {
    val socketStream: DStream[String] = ssc.socketTextStream("localhost", 12345)

    // transformation = lazy
    val wordsStream: DStream[String] = socketStream.flatMap(line => line.split(" "))

    // action
    //wordsStream.print()
    wordsStream.saveAsTextFiles("src/main/resources/data/words") // each folder is an RDD, each file = partition of RDD

    ssc.start()
    ssc.awaitTermination()
  }

  def readFromFile(): Unit = {
    val stocksFilePath: String = "src/main/resources/data/stocks"

    // textFileStream monitors a directory for new files
    val textStream: DStream[String] = ssc.textFileStream(stocksFilePath)

    // transformations
    val dateFormat = new SimpleDateFormat("MMM d yyyy")

    val stocksStream: DStream[Stock] = textStream.map { line =>
      val tokens = line.split(",")
      val company = tokens(0)
      val date = new Date(dateFormat.parse(tokens(1)).getTime)
      val price = tokens(2).toDouble
      
      Stock(company = company, date = date, value = price)
    }

    stocksStream.print()

    ssc.start()
    ssc.awaitTermination()
  }

  def main(args: Array[String]): Unit = {
    readFromSocket()
  }
}

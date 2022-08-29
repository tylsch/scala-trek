package part2structuredstreaming

import common.stocksSchema
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming.Trigger

import scala.concurrent.duration._

object StreamingDataFrames {
  val spark = SparkSession.builder()
    .appName("Our first stream")
    .master("local[2]")
    .getOrCreate()

  def readFromSocket() ={
    // reading DF
    val lines = spark.readStream
      .format("socket")
      .option("host", "localhost")
      .option("port", "12345")
      .load()

    // transformations
    val shortLines = lines.filter(length(col("value")) <= 5)

    println(shortLines.isStreaming)

    // consuming a DF
    val query = shortLines.writeStream
      .format("console")
      .outputMode("append")
      .start()

    // wait for stream to finish
    query.awaitTermination()
  }

  def readFromFiles() = {
    val stocksDF = spark.readStream
      .option("header", "false")
      .option("dateFormat", "MMM d yyyy")
      .schema(stocksSchema)
      .csv("src/main/resources/data/stocks")

    stocksDF.writeStream
      .format("console")
      .outputMode("append")
      .start()
      .awaitTermination()
  }

  def demoTriggers() = {
    val lines = spark.readStream
      .format("socket")
      .option("host", "localhost")
      .option("port", "12345")
      .load()

    // write the lines DF at a certain trigger
    lines.writeStream
      .format("console")
      .outputMode("append")
      .trigger(
        //Trigger.ProcessingTime(2.seconds) // every 2 seconds run the query
        //Trigger.Once() // single batch, then terminate
        Trigger.Continuous(2.seconds) // experimental, every 2 seconds create a batch with whatever you have
      )
      .start()
      .awaitTermination()


  }

  def main(args: Array[String]): Unit = {
    demoTriggers()
  }
}

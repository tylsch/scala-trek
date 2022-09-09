package part6advanced

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._

object ProcessingTimeWindows {
  val spark: SparkSession = SparkSession.builder()
    .appName("Processing Time Windows")
    .master("local[*]")
    .getOrCreate()

  spark.sparkContext.setLogLevel("ERROR")

  def aggregateByProcessingTime(): Unit = {
    val linesDF: DataFrame = spark.readStream
      .format("socket")
      .option("host", "localhost")
      .option("port", 12345)
      .load()
      .select(col("value"), current_timestamp().as("processingTime"))
      .groupBy(window(col("processingTime"), "10 seconds").as("window"))
      .agg(sum(length(col("value"))).as("charCount")) // counting characters every 10 seconds by processing time
      .select(
        col("window").getField("start").as("start"),
        col("window").getField("end").as("end"),
        col("charCount")
      )

    linesDF.writeStream
      .format("console")
      .outputMode("complete")
      .start()
      .awaitTermination()
  }

  def main(args: Array[String]): Unit = {
    aggregateByProcessingTime()
  }
}

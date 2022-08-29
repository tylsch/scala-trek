package part2structuredstreaming

import org.apache.spark.sql.{Column, SparkSession}
import org.apache.spark.sql.functions._

object StreamingAggregations {
  val spark = SparkSession.builder()
    .appName("Streaming Aggregations")
    .master("local[2]")
    .getOrCreate()

  def streamingCount() = {
    val lines = spark.readStream
      .format("socket")
      .option("host", "localhost")
      .option("port", "12345")
      .load()

    val lineCount = lines.selectExpr("count(*) as lineCount")

    // aggregations with distinct are not supported
    // otherwise Spark will need to keep track of everything

    lineCount.writeStream
      .format("console")
      .outputMode("complete") // append and update not supported on aggregations without watermarks
      .start()
      .awaitTermination()
  }

  def numericalAggregations(aggFunction: Column => Column): Unit = {
    val lines = spark.readStream
      .format("socket")
      .option("host", "localhost")
      .option("port", "12345")
      .load()

    val numbers = lines.select(col("value").cast("integer").as("number"))
    val aggDF = numbers.select(aggFunction(col("number")).as("agg_so_far"))

    aggDF.writeStream
      .format("console")
      .outputMode("complete")
      .start()
      .awaitTermination()
  }

  def groupNames() = {
    val lines = spark.readStream
      .format("socket")
      .option("host", "localhost")
      .option("port", "12345")
      .load()

    // counting each occurrence of the "name" value
    val names = lines
      .select(col("value").as("name"))
      .groupBy(col("name")) // RelationalGroupDataset
      .count()

    names.writeStream
      .format("console")
      .outputMode("complete")
      .start()
      .awaitTermination()
  }

  def main(args: Array[String]): Unit = {
    groupNames()
  }
}

package part6advanced

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.{IntegerType, StringType, StructField, StructType, TimestampType}

object EventTimeWindows {
  val spark: SparkSession = SparkSession.builder()
    .appName("Event Time Windows")
    .master("local[2]")
    .getOrCreate()

  spark.sparkContext.setLogLevel("ERROR")

  val onlinePurchaseSchema: StructType = StructType(Array(
    StructField("id", StringType),
    StructField("time", TimestampType),
    StructField("item", StringType),
    StructField("quantity", IntegerType)
  ))

  def readPurchasesFromSocket(): DataFrame = spark.readStream
    .format("socket")
    .option("host", "localhost")
    .option("port", 12345)
    .load()
    .select(from_json(col("value"), onlinePurchaseSchema).as("purchase"))
    .selectExpr("purchase.*")

  def readPurchasesFromFile(): DataFrame = spark.readStream
    .schema(onlinePurchaseSchema)
    .json("src/main/resources/data/purchases")

  def aggregatePurchasesBySlidingWindow(): Unit = {
    val purchasesDF = readPurchasesFromSocket()

    val windowByDay: DataFrame = purchasesDF
      .groupBy(window(col("time"), "1 day", "1 hour").as("time")) // struct column: has fields {start, end}
      .agg(sum("quantity").as("totalQuantity"))
      .select(
        col("time").getField("start").as("start"),
        col("time").getField("end").as("end"),
        col("totalQuantity")
      )

    windowByDay.writeStream
      .format("console")
      .outputMode("complete")
      .start()
      .awaitTermination()
  }

  def aggregatePurchasesByTumblingWindow(): Unit = {
    val purchasesDF = readPurchasesFromSocket()

    val windowByDay: DataFrame = purchasesDF
      .groupBy(window(col("time"), "1 day").as("time")) // struct column: has fields {start, end}
      .agg(sum("quantity").as("totalQuantity"))
      .select(
        col("time").getField("start").as("start"),
        col("time").getField("end").as("end"),
        col("totalQuantity")
      )

    windowByDay.writeStream
      .format("console")
      .outputMode("complete")
      .start()
      .awaitTermination()
  }

  /*
  * 1) Show the best selling product of every day (with quantity)
  * 2) Show the best selling product of every 24 hours, updated every hour
  * */

  def bestSellingProductPerDay(): Unit = {
    val purchasesDF = readPurchasesFromFile()

    val bestSelling: DataFrame = purchasesDF
      .groupBy(col("item"), window(col("time"), "1 day").as("day")) // struct column: has fields {start, end}
      .agg(sum("quantity").as("totalQuantity"))
      .select(
        col("day").getField("start").as("start"),
        col("day").getField("end").as("end"),
        col("item"),
        col("totalQuantity")
      )
      .orderBy(col("start"), col("totalQuantity").desc)

    bestSelling.writeStream
      .format("console")
      .outputMode("complete")
      .start()
      .awaitTermination()
  }

  def bestSellingProductPerDayPerHour(): Unit = {
    val purchasesDF = readPurchasesFromFile()

    val bestSelling: DataFrame = purchasesDF
      .groupBy(col("item"), window(col("time"), "1 day", "1 hour").as("time")) // struct column: has fields {start, end}
      .agg(sum("quantity").as("totalQuantity"))
      .select(
        col("time").getField("start").as("start"),
        col("time").getField("end").as("end"),
        col("item"),
        col("totalQuantity")
      )
      .orderBy(col("start"), col("totalQuantity").desc)

    bestSelling.writeStream
      .format("console")
      .outputMode("complete")
      .start()
      .awaitTermination()
  }

  /*
  * For window functions, windows start at 1/1/1970 12 AM GMT
  * */

  def main(args: Array[String]): Unit = {
    bestSellingProductPerDayPerHour()
  }
}

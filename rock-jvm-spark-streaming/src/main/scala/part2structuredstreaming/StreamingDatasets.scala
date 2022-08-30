package part2structuredstreaming

import common.{Car, carsSchema}
import org.apache.spark.sql.{Dataset, Encoders, SparkSession}
import org.apache.spark.sql.functions._

object StreamingDatasets {
  val spark: SparkSession = SparkSession.builder()
    .appName("Streaming Datasets")
    .master("local[2]")
    .getOrCreate()

  import spark.implicits._
  def readCars(): Dataset[Car] = {
    val carEncoder = Encoders.product[Car]
    spark.readStream
      .format("socket")
      .option("host", "localhost")
      .option("port", "12345")
      .load()
      .select(from_json(col("value"), carsSchema).as("car")) // composite column (struct)
      .selectExpr("car.*") // DF with multiple columns
      .as[Car](carEncoder)
  }

  def showCarNames() = {
    val carsDS = readCars()

    // transformations here
    val carNamesDF = carsDS.select(col("Name"))

    // collection transformations maintain type info
    val carNamesAltDF: Dataset[String] = carsDS.map(_.Name)

    carNamesAltDF.writeStream
      .format("console")
      .outputMode("append")
      .start()
      .awaitTermination()
  }

  /*
    * Exercises
    * 1) Count how many POWERFUL cars we have in the DS, HP > 140
    * 2) Average HP for the entire dataset
    * 3) Count the cars by their Origin field
    * */

  def countPowerfulCars() = {
    val carsDS = readCars()

    carsDS.filter(_.Horsepower.getOrElse(0L) >= 140)
      .writeStream
      .format("console")
      .outputMode("append")
      .start()
      .awaitTermination()
  }

  def avgHorsePower() = {
    val carsDS = readCars()

    carsDS.select(avg(col("Horsepower")))
      .writeStream
      .format("console")
      .outputMode("complete")
      .start()
      .awaitTermination()
  }

  def originCount() = {
    val carsDS = readCars()

    val option1 = carsDS.groupBy(col("Origin")).count()

    // With DS API
    carsDS.groupByKey(car => car.Origin).count()
      .writeStream
      .format("console")
      .outputMode("complete")
      .start()
      .awaitTermination()
  }



  def main(args: Array[String]): Unit = {
    originCount()
  }
}

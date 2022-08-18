package part3typesdatasets

import org.apache.spark.sql.functions._
import org.apache.spark.sql.{Dataset, Encoders, SparkSession}

object Datasets extends App {
  val spark = SparkSession.builder()
    .appName("Datasets")
    .config("spark.master", "local")
    .getOrCreate()

  val numbersDF = spark.read
    .options(Map(
      "header" -> "true",
      "inferSchema" -> "true"
    ))
    .csv("src/main/resources/data/numbers.csv")

  numbersDF.printSchema()

  // convert DF to a Dataset
  implicit val intEncoder = Encoders.scalaInt
  val numbersDS: Dataset[Int] = numbersDF.as[Int]

  // dataset of a complex type
  // 1 - define your case class
  case class Car(
                  Name: String,
                  Miles_per_Gallon: Option[Double],
                  Cylinders: Long,
                  Displacement: Double,
                  Horsepower: Option[Long],
                  Weight_in_lbs: Long,
                  Acceleration: Double,
                  Year: String,
                  Origin: String
                )

  // 2 - read DF from file
  def readDF(fileName: String) = spark.read
    .option("inferSchema", "true")
    .json(s"src/main/resources/data/$fileName")

  // 3 - Define an encoder (import implicits)

  import spark.implicits._

  // 4 - convert DF to DS
  val carsDF = readDF("cars.json")
  val carsDS = carsDF.as[Car]

  // DS collection functions
  numbersDS.filter(_ < 100)

  // map, flatMap, fold, reduce, ...
  val carNamesDS = carsDS.map(car => car.Name.toUpperCase())

  /*
  * Exercise
  * 1) Count how many cars we have
  * 2) Count how may POWERFUL cars we have (HP > 140)
  * 3) Average HP for the entire dataset
  * */
  val carsCount = carsDS.count
  println(carsCount)
  println(carsDS.filter(_.Horsepower.getOrElse(0L) > 140).count)
  println(carsDS.map(_.Horsepower.getOrElse(0L)).reduce(_ + _) / carsCount)
  carsDS.select(avg(col("Horsepower"))).show() // can also use DF functions
}

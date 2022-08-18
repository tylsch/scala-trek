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

  // Joins
  case class Guitar(id: Long, make: String, guitarType: String)
  case class GuitarPlayer(id: Long, name: String, guitars: Seq[Long], band: Long)
  case class Band(id: Long, name: String, hometown: String, year: Long)

  val guitarsDS = readDF("guitars.json").as[Guitar]
  val guitarPlayersDS = readDF("guitarPlayers.json").as[GuitarPlayer]
  val bandsDS = readDF("bands.json").as[Band]

  val guitarPlayerBandsDS: Dataset[(GuitarPlayer, Band)] = guitarPlayersDS.joinWith(bandsDS, guitarPlayersDS.col("band") === bandsDS.col("id"), "inner")

  /*
  * Exercise:
  * join the guitarDS and guitarPlayerDS, in an outer join
  * */

  guitarPlayersDS.joinWith(guitarsDS, array_contains(guitarPlayersDS.col("guitars"), guitarsDS.col("id")), "outer")

  // Grouping
  val carsGroupedByOrigin = carsDS.groupByKey(_.Origin).count()
  carsGroupedByOrigin.show()

  // joins and groups are WIDE transformations, will involve SHUFFLE operations
}

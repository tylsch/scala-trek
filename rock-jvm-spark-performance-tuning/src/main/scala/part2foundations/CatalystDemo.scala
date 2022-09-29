package part2foundations

import org.apache.spark.sql.{DataFrame, SparkSession}

object CatalystDemo {
  val spark = SparkSession.builder()
    .appName("Catalyst Demo")
    .master("local[*]")
    .getOrCreate()

  spark.sparkContext.setLogLevel("ERROR")

  import spark.implicits._

  val flights = spark.read
    .option("inferSchema", "true")
    .json("src/main/resources/data/flights")

  val notFromHere = flights
    .where($"origin" =!= "LGA")
    .where($"origin" =!= "ORD")
    .where($"origin" =!= "SFO")
    .where($"origin" =!= "DEN")
    .where($"origin" =!= "BOS")
    .where($"origin" =!= "EWR")

  notFromHere.explain(true)

  def filterFromTeam1(flights: DataFrame) = flights.where($"origin" =!= "LGA").where($"dest" === "DEN")
  def filterFromTeam2(flights: DataFrame) = flights.where($"origin" =!= "EWR").where($"dest" === "DEN")

  val filterBoth = filterFromTeam1(filterFromTeam2((flights)))
  filterBoth.explain(true)

  // pushing down filters
  flights.write.save("src/main/resources/data/flights_parquet")

  val notFromLGA = spark.read.load("src/main/resources/data/flights_parquet")
    .where($"origin" =!= "LGA")

  notFromLGA.explain

  def main(args: Array[String]): Unit = {

  }
}

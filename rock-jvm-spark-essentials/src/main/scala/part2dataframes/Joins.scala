package part2dataframes

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.expr

object Joins extends App {
  val spark = SparkSession.builder()
    .appName("Joins")
    .config("spark.master", "local")
    .getOrCreate()

  val guitarsDF = spark.read
    .option("inferSchema", "true")
    .json("src/main/resources/data/guitars.json")

  val guitarPlayersDF = spark.read
    .option("inferSchema", "true")
    .json("src/main/resources/data/guitarPlayers.json")

  val bandsDF = spark.read
    .option("inferSchema", "true")
    .json("src/main/resources/data/bands.json")

  // joins
  val joinCondition = guitarPlayersDF.col("band") === bandsDF.col("id")
  val guitaristBandsDF = guitarPlayersDF
    .join(
      bandsDF,
      joinCondition,
      "inner"
    )

  // left outer join, with nulls where data is missing
  guitarPlayersDF
    .join(
      bandsDF,
      joinCondition,
      "left_outer"
    )

  //right outer join
  guitarPlayersDF
    .join(
      bandsDF,
      joinCondition,
      "right_outer"
    )

  // full outer join
  guitarPlayersDF
    .join(
      bandsDF,
      joinCondition,
      "full_outer"
    )

  // semi-joins = everything in the left DF for which there is a row in the right DF satisfying the condition
  guitarPlayersDF
    .join(
      bandsDF,
      joinCondition,
      "left_semi"
    )

  // anti-join = everything in the left DF for which there is a NO row in the right DF satisfying the condition
  guitarPlayersDF
    .join(
      bandsDF,
      joinCondition,
      "left_anti"
    )

  // things to bear in mind with joins
//  guitarPlayersDF.select("id", "band") // this crashes

  // option 1 - rename the column on which we are joining
  guitarPlayersDF
    .join(
      bandsDF.withColumnRenamed("id", "band"),
      "band"
    )

  // option 2 - drop the dup column
  guitaristBandsDF.drop(bandsDF.col("id"))

  // option 3 - rename the offending column and keep the data
  val bandsModDF = bandsDF.withColumnRenamed("id", "band")
  guitarPlayersDF.join(bandsDF, guitarPlayersDF.col("band") === bandsModDF.col("id"))

  // using complex types
  guitarPlayersDF
    .join(
      guitarsDF.withColumnRenamed("id", "guitarId"),
      expr("array_contains(guitars, guitarId)")
    )
}

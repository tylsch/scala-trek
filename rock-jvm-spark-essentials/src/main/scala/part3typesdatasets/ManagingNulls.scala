package part3typesdatasets

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

object ManagingNulls extends App {
  val spark = SparkSession.builder()
    .appName("Managing Nulls")
    .config("spark.master", "local")
    .getOrCreate()

  val moviesDF = spark.read
    .option("inferSchema", "true")
    .json("src/main/resources/data/movies.json")

  // select the first non-null value
  moviesDF.select(
    col("Title"),
    col("Rotten_Tomatoes_Rating"),
    col("IMDB_Rating"),
    coalesce(col("Rotten_Tomatoes_Rating"), col("IMDB_Rating") * 10).as("Rating")
  )

  // checking for nulls
  moviesDF.select("*").where(col("Rotten_Tomatoes_Rating").isNull)

  // nulls whe ordering
  moviesDF.orderBy(col("IMDB_Rating").desc_nulls_last)

  // remove or replace nulls
  moviesDF.select(col("Title"), col("IMDB_Rating")).na.drop() // remove nulls
  moviesDF.na.fill(0, List("IMDB_Rating", "Rotten_Tomatoes_Rating")) // replace values
  moviesDF.na.fill(Map(
    "IMDB_Rating" -> 0,
    "Rotten_Tomatoes_Rating" -> 10,
    "Director" -> "Unknown"
  ))

  // complex operations
  moviesDF.selectExpr(
    "Title",
    "IMDB_Rating",
    "Rotten_Tomatoes_Rating",
    "ifnull(Rotten_Tomatoes_Rating, IMDB_Rating * 10) as ifnull", // same as coalesce
    "nvl(Rotten_Tomatoes_Rating, IMDB_Rating * 10) as nvl",
    "nullif(Rotten_Tomatoes_Rating, IMDB_Rating * 10) as nullif", // returns null if the two values are EQUAL, else first value
    "nvl2(Rotten_Tomatoes_Rating, IMDB_Rating * 10, 0.0) as nvl2" // if (first != null) second else third
  ).show()
}

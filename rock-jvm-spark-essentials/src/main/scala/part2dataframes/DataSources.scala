package part2dataframes

import org.apache.spark.sql.{SaveMode, SparkSession}
import org.apache.spark.sql.types.{DateType, DoubleType, LongType, StringType, StructField, StructType}
import part2dataframes.DataFramesBasics.spark

object DataSources extends App {
  val spark = SparkSession.builder()
    .appName("DataSourcesAndFormats")
    .config("spark.master", "local")
    .getOrCreate()

  // schema
  val carsSchema = StructType(Array(
    StructField("Name", StringType),
    StructField("Miles_per_Gallon", DoubleType),
    StructField("Cylinders", LongType),
    StructField("Displacement", DoubleType),
    StructField("Horsepower", LongType),
    StructField("Weight_in_lbs", LongType),
    StructField("Acceleration", DoubleType),
    StructField("Year", DateType),
    StructField("Origin", StringType)
  ))

  /*
  * Reading a DF:
  * - format
  * - schema(optional)
  * - zero or more options
  * */

  val carsDF = spark.read
    .format("json")
    .schema(carsSchema) // enforce a schema
    .option("mode", "failFast") // dropMalformed, permissive (default)
    .load("src/main/resources/data/cars.json")

//  carsDF.show()

  // alternative reading with options map
  val carsDFWithOptionMap = spark.read
    .format("json")
    .options(Map(
      "mode" -> "failFast",
      "path" -> "src/main/resources/data/cars.json",
      "inferSchema" -> "true"
    ))
    .load()

  /*
  * Writing DFs
  * - format
  * - save mode = overwrite, append, ignore, errorIfExits
  * */
//  carsDF.write
//    .format("json")
//    .mode(SaveMode.Overwrite)
//    .save("src/main/resources/data/cars_dupe.json")

  // JSON flags
  spark.read
    .schema(carsSchema)
    .options(Map(
      "dateFormat" -> "YYYY-MM-dd", // couple with schema, if Spark fails parsing, it will put null
      "allowSingleQuotes" -> "true",
      "compression" -> "uncompressed" // bzip2, gzip, lz4, snappy, and deflate
    ))
    .json("src/main/resources/data/cars.json")

  // CSV flags
  val stocksSchema = StructType(Array(
    StructField("symbol", StringType),
    StructField("date", DateType),
    StructField("price", DoubleType)
  ))

  spark.read
    .schema(stocksSchema)
    .options(Map(
      "dateFormat" -> "MMM dd YYYY",
      "header" -> "true",
      "sep" -> ",",
      "nullValue" -> ""
    ))
    .csv("src/main/resources/data/stocks.csv")

  // Parquet
//  carsDF.write
//    .mode(SaveMode.Overwrite)
//    .parquet("src/main/resources/data/cars.parquet")

  // Text Files
  spark.read
    .text("src/main/resources/data/sample_text.txt")
    .show()

  // Reading from a remote DB
  val employeesDF = spark.read
    .format("jdbc")
    .options(Map(
      "driver" -> "org.postgresql.Driver",
      "url" -> "jdbc:postgresql://localhost:5432/rtjvm",
      "user" -> "docker",
      "password" -> "docker",
      "dbtable" -> "public.employees"
    ))
    .load()

  employeesDF.show()

  /*
  * Exercise: read the movies DF, then write is as
  * - tab-separated values
  * - snappy Parquet
  * - table in Postgres DB called public.movies
  * */

  val moviesDF = spark.read
    .format("json")
    .option("inferSchema", "true")
    .load("src/main/resources/data/movies.json")

  moviesDF.write
    .options(Map(
      "header" -> "true",
      "sep" -> "\t"
    ))
    .csv("src/main/resources/data/movies.csv")

  moviesDF.write
    .save("src/main/resources/data/movies.parquet")

  moviesDF.write
    .format("jdbc")
    .options(Map(
      "driver" -> "org.postgresql.Driver",
      "url" -> "jdbc:postgresql://localhost:5432/rtjvm",
      "user" -> "docker",
      "password" -> "docker",
      "dbtable" -> "public.movies"
    ))
    .save()
}

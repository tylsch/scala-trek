package part2dataframes

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.{col, column, expr}

object ColumnsAndExpressions extends App {
  val spark = SparkSession.builder()
    .appName("Columns and Expressions")
    .config("spark.master", "local")
    .getOrCreate()

  val carsDF = spark.read
    .options(Map(
      "inferSchema" -> "true"
    ))
    .json("src/main/resources/data/cars.json")

  carsDF.show()

  // Columns
  val firstColumn = carsDF.col("Name")

  // selecting (projecting)
  val carNamesDF = carsDF.select(firstColumn)
//  carNamesDF.show()

  // various select methods
  import spark.implicits._
  carsDF.select(
    carsDF.col("Name"),
    col("Acceleration"),
    column("Weight_in_lbs"),
    'Year,
    $"Horsepower",
    expr("Origin") // EXPRESSION
  )

  // selecting with plain column names
  carsDF.select("Name", "Year")

  // EXPRESSIONS
  val simplestExpression = carsDF.col("Weight_in_lbs")
  val weightInKgExpression = carsDF.col("Weight_in_lbs") / 2.2

  val carsWithWeightsDF = carsDF.select(
    col("Name"),
    col("Weight_in_lbs"),
    weightInKgExpression.as("Weight_in_kg"),
    expr("Weight_in_lbs / 2.2").as("Weight_in_kg_2")
  )

//  carsWithWeightsDF.show()

  val carsWithSelectExprWeights = carsDF.selectExpr(
    "Name",
    "Weight_in_lbs",
    "Weight_in_lbs / 2.2"
  )

  // DF processing
  val carsWithKg3DF = carsDF.withColumn("Weight_in_kg_3", col("Weight_in_lbs") / 2.2)
  val carsWithColumnRenamed = carsDF.withColumnRenamed("Weight_in_lbs", "Weight_in_pounds")
//  carsWithColumnRenamed.selectExpr("`Weight in pounds`")
  carsWithColumnRenamed.drop("Cylinders", "Displacement")

  // filtering
  val europeanCarsDF = carsDF.filter(col("Origin") =!= "USA")
  val europeanCars2DF = carsDF.where(col("Origin") =!= "USA")
  val americanCarsDF = carsDF.filter("Origin = 'USA'")
  val americanPowerfulCarsDF = carsDF.filter(col("Origin") === "USA").filter(col("Horsepower") > 150)
  val americanPowerfulCars2DF = carsDF.filter(col("Origin") === "USA" and col("Horsepower") > 150)
  val americanPowerfulCars3DF = carsDF.filter("Origin = 'USA' and Horsepower > 150")

  // unioning = adding more rows
  val moreCarsDF = spark.read.option("inferSchema", "true").json("src/main/resources/data/more_cars.json")
  val allCarsDF = carsDF.union(moreCarsDF)

  // distinct values
  val allCountriesDF = carsDF.select("Origin").distinct()
  allCountriesDF.show()

  /*
  * Exercises
  * 1) Read the movies DF and select 2 columns of your choice
  * 2) Create another column summing up the total profit of the movies
  * 3) Select all the comedy movies IMDB Rating above 6
  *
  * Use as many versions as possible
  * */

  val moviesDF = spark.read
    .options(Map(
      "inferSchema" -> "true"
    ))
    .json("src/main/resources/data/movies.json")

  val moviesSelectDF = moviesDF.select("Title", "US_Gross")
  moviesSelectDF.show()

  val totalProfitsDF = moviesDF.select(
    col("Title"),
    col("US_Gross"),
    col("Worldwide_Gross"),
    col("US_DVD_Sales"),
    (col("US_Gross") + col("Worldwide_Gross")).as("Total_Gross")
  )
  totalProfitsDF.show()

  val bestComediesDF = moviesDF
    .select("Title", "IMDB_Rating")
    .where("Major_Genre = 'Comedy' and IMDB_Rating >= 6")
  bestComediesDF.show()
}

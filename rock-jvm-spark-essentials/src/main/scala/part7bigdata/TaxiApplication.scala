package part7bigdata

import org.apache.spark.sql.SparkSession

object TaxiApplication extends App {
  val spark = SparkSession.builder()
    .appName("Taxi Big Data Application")
    .config("spark.master", "local")
    .getOrCreate()

  val taxiDF = spark.read.load("src/main/resources/data/yellow_taxi_jan_25_2018")
  taxiDF.printSchema()

  val taxiZonesDF = spark.read
    .option("header", "true")
    .option("inferSchema", "true")
    .csv("src/main/resources/data/taxi_zones.csv")

  taxiZonesDF.printSchema()
}

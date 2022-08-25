package part7bigdata

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.{avg, col, count, from_unixtime, round, sum, unix_timestamp}

object TaxiEconomicImpact extends App {
  val spark = SparkSession.builder()
    .appName("Taxi Big Data Application")
    .config("spark.master", "local")
    .getOrCreate()

  import spark.implicits._

  val taxiDF = spark.read.load("src/main/resources/data/yellow_taxi_jan_25_2018")
  taxiDF.printSchema()

  val taxiZonesDF = spark.read
    .option("header", "true")
    .option("inferSchema", "true")
    .csv("src/main/resources/data/taxi_zones.csv")

  val percentGroupAttempt = 0.05
  val percentAcceptGrouping = 0.3
  val discount = 5
  val extraCost = 2
  val avgCostReduction = 0.6 * taxiDF.select(avg(col("total_amount"))).as[Double].take(1)(0)
  val percentGroupable = 28923 * 1.0 / 331983

  val groupAttemptsDF = taxiDF
    .select(round(unix_timestamp(col("tpep_pickup_datetime")) / 300).cast("integer").as("fiveMinId"), col("PULocationID"), col("total_amount"))
    .where(col("passenger_count") < 3)
    .groupBy(col("fiveMinId"), col("PULocationID"))
    .agg(count("*").as("totalTrips"), sum(col("total_amount")).as("total_amount"))
    .withColumn("approximate_datetime", from_unixtime(col("fiveMinId") * 300))
    .drop("fiveMinId")
    .join(taxiZonesDF, col("PULocationID") === col("LocationID"))
    .drop("LocationID", "service_zone")
    .orderBy(col("totalTrips").desc_nulls_last)


  val groupingEstimatingEconomicImpactIDF = groupAttemptsDF
    .withColumn("groupedRides", col("totalTrips") * percentGroupAttempt)
    .withColumn("acceptedGroupRidesEconomicImpact", col("groupedRides") * percentAcceptGrouping * (avgCostReduction - discount))
    .withColumn("rejectedGroupedRidesEconomicImpact", col("groupedRides") * (1 - percentAcceptGrouping) * extraCost)
    .withColumn("totalImpact", col("acceptedGroupRidesEconomicImpact") + col("rejectedGroupedRidesEconomicImpact"))

  val totalProfitDF = groupingEstimatingEconomicImpactIDF
    .select(sum(col("totalImpact")).as("total"))


}

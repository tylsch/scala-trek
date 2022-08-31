package part4integrations

import common.carsSchema
import org.apache.spark.sql.functions._
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.streaming.{Seconds, StreamingContext}

object IntegratingKafka {
  val spark: SparkSession = SparkSession.builder()
    .appName("Integrating Kafka")
    .master("local[2]")
    .getOrCreate()

  spark.sparkContext.setLogLevel("ERROR")

  val ssc = new StreamingContext(sparkContext = spark.sparkContext, batchDuration = Seconds(1))

  def readFromKafka(): Unit = {
    val kafkaDF: DataFrame = spark.readStream
      .format("kafka")
      .options(Map(
        "kafka.bootstrap.servers" -> "localhost:9092",
        "subscribe" -> "rockthejvm"
      ))
      .load()

    kafkaDF
      .select(col("topic"), expr("cast(value as string) as actualValue"))
      .writeStream
      .format("console")
      .outputMode("append")
      .start()
      .awaitTermination()
  }

  def writeToKafka(): Unit = {
    val carsDF = spark.readStream
      .schema(carsSchema)
      .json("src/main/resources/data/cars")

    val carsKafkaDF = carsDF.selectExpr("upper(Name) as key", "Name as value")

    carsKafkaDF
      .writeStream
      .format("kafka")
      .options(Map(
        "kafka.bootstrap.servers" -> "localhost:9092",
        "topic" -> "rockthejvm",
        "checkpointLocation" -> "src/main/resources/data/checkpoints"
      ))
      .start()
      .awaitTermination()
  }

  def writeCarsToKafka(): Unit = {
    val carsDF = spark.readStream
      .schema(carsSchema)
      .json("src/main/resources/data/cars")

    val carsKafkaDF = carsDF.select(
      col("Name").as("key"),
      to_json(struct(col("*"))).cast("String").as("value")
    )

    carsKafkaDF
      .writeStream
      .format("kafka")
      .options(Map(
        "kafka.bootstrap.servers" -> "localhost:9092",
        "topic" -> "rockthejvm",
        "checkpointLocation" -> "src/main/resources/data/checkpoints"
      ))
      .start()
      .awaitTermination()
  }

  def main(args: Array[String]): Unit = {
    writeCarsToKafka()
  }
}

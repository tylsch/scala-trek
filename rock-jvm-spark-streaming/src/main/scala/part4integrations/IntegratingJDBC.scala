package part4integrations

import common.{Car, carsSchema}
import org.apache.spark.sql.{Dataset, SaveMode, SparkSession}
import org.apache.spark.streaming.{Seconds, StreamingContext}

object IntegratingJDBC {
  val spark: SparkSession = SparkSession.builder()
    .appName("Integrating JDBC")
    .master("local[2]")
    .getOrCreate()

  spark.sparkContext.setLogLevel("ERROR")

  val ssc = new StreamingContext(sparkContext = spark.sparkContext, batchDuration = Seconds(1))

  val driver = "org.postgresql.Driver"
  val url = "jdbc:postgresql://localhost:5432/rtjvm"
  val user = "docker"
  val password = "docker"

  import spark.implicits._

  def writeStreamToPostgres(): Unit = {
    val carsDF = spark.readStream
      .schema(carsSchema)
      .json("src/main/resources/data/cars")

    val carsDS = carsDF.as[Car]

    carsDS.writeStream
      .foreachBatch { (batch: Dataset[Car], _: Long) =>
        // each executor can control the batch
        // batch is a STATIC Dataset/DataFrame

        batch.write
          .format("jdbc")
          .options(Map(
            "driver" -> driver,
            "url" -> url,
            "user" -> user,
            "password" -> password,
            "dbtable" -> "public.cars"
          ))
          .mode(SaveMode.Append)
          .save()
      }
      .start()
      .awaitTermination()
  }

  def main(args: Array[String]): Unit = {
    writeStreamToPostgres()
  }
}

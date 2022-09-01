package part4integrations

import com.datastax.spark.connector.cql.CassandraConnector
import common.{Car, carsSchema}
import org.apache.spark.sql.cassandra._
import org.apache.spark.sql.functions._
import org.apache.spark.sql.{Dataset, ForeachWriter, SaveMode, SparkSession}
import org.apache.spark.streaming.{Seconds, StreamingContext}

object IntegratingCassandra {
  val spark: SparkSession = SparkSession.builder()
    .appName("Integrating Cassandra")
    .master("local[2]")
    .getOrCreate()

  spark.sparkContext.setLogLevel("ERROR")

  val ssc = new StreamingContext(sparkContext = spark.sparkContext, batchDuration = Seconds(1))

  import spark.implicits._

  def writeStreamToCassandraInBatches(): Unit = {
    val carDS: Dataset[Car] = spark.readStream
      .schema(carsSchema)
      .json("src/main/resources/data/cars")
      .as[Car]

    carDS
      .writeStream
      .foreachBatch { (batch: Dataset[Car], _: Long) =>
        // save this batch to Cassandra in a single transaction
        batch
          .select(col("Name"), col("Horsepower"))
          .write
          .cassandraFormat("cars", "public") // type enrichment
          .mode(SaveMode.Append)
          .save()
      }
      .start()
      .awaitTermination()
  }

  class CarCassandraForeachWriter extends ForeachWriter[Car] {
    /*
    * - on every batch, on every partition `partitionId`
    *   - on every "epoch" = chuck of data
    *     - call the open method; if false, skip this chunk
    *     - foreach entry in this chunk, call the process method
    *     - call the close method either at the end of the chunk or an error
    * */

    val keyspace = "public"
    val table = "cars"
    val connector: CassandraConnector = CassandraConnector(spark.sparkContext.getConf)

    override def open(partitionId: Long, epochId: Long): Boolean = {
      println("Open Connection")
      true
    }
    override def process(value: Car): Unit = {
      connector.withSessionDo{ session =>
        session.execute(
          s"""
             |insert into $keyspace.$table("Name", "Horsepower")
             |values ('${value.Name}', ${value.Horsepower.orNull})
             |""".stripMargin)
      }
    }
    override def close(errorOrNull: Throwable): Unit = println("Closing connection")
  }

  def writeStreamToCassandra(): Unit = {
    val carDS: Dataset[Car] = spark.readStream
      .schema(carsSchema)
      .json("src/main/resources/data/cars")
      .as[Car]

    carDS
      .writeStream
      .foreach(new CarCassandraForeachWriter)
      .start()
      .awaitTermination()
  }

  def main(args: Array[String]): Unit = {
    writeStreamToCassandra()
  }
}

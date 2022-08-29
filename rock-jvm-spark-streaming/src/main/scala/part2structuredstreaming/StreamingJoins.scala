package part2structuredstreaming

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._

object StreamingJoins {
  val spark: SparkSession = SparkSession.builder()
    .appName("Streaming Joins")
    .master("local[2]")
    .getOrCreate()

  val guitarPlayers: DataFrame = spark.read
    .option("inferSchema", "true")
    .json("src/main/resources/data/guitarPlayers/guitarPlayers.json")

  val guitars: DataFrame = spark.read
    .option("inferSchema", "true")
    .json("src/main/resources/data/guitars/guitars.json")

  val bands: DataFrame = spark.read
    .option("inferSchema", "true")
    .json("src/main/resources/data/bands/bands.json")

  val joinCondition = guitarPlayers.col("band") === bands.col("id")
  val guitaristsBands = guitarPlayers.join(bands, joinCondition , "inner")

  val bandsSchema = bands.schema

  def joinStreamWithStatic() = {
    val streamedBandsDF = spark.readStream
      .format("socket")
      .option("host", "localhost")
      .option("port", "12345")
      .load()
      .select(from_json(col("value"), bandsSchema).as("band"))
      .selectExpr("band.id as id", "band.name as name", "band.hometown as hometown", "band.year as year")

    // join happens PER BATCH
    val streamedBandsGuitaristsDF = streamedBandsDF.join(guitarPlayers, guitarPlayers.col("band") === streamedBandsDF.col("id"), "inner")

    /*
    * Restricted Joins:
    * - STREAM JOINING WITH STATIC: RIGHT OUTER JOIN/FULL OUTER JOIN & JOIN/RIGHT_SEMI NOT PERMITTED
    * - static joining with streaming: LEFT outer join/full/left_semi not permitted
    * */
    streamedBandsGuitaristsDF.writeStream
      .format("console")
      .outputMode("append")
      .start()
      .awaitTermination()
  }

  // Since Spark 2.3 we have stream vs stream joins
  def joinStreamWithStream() = {
    val streamedBandsDF = spark.readStream
      .format("socket")
      .option("host", "localhost")
      .option("port", "12345")
      .load()
      .select(from_json(col("value"), bandsSchema).as("band"))
      .selectExpr("band.id as id", "band.name as name", "band.hometown as hometown", "band.year as year")

    val streamedGuitaristDF = spark.readStream
      .format("socket")
      .option("host", "localhost")
      .option("port", "12346")
      .load()
      .select(from_json(col("value"), guitarPlayers.schema).as("guitarPlayer"))
      .selectExpr("guitarPlayer.id as id", "guitarPlayer.name as name", "guitarPlayer.guitars as guitars", "guitarPlayer.band as band")

    // join stream with a stream
    val streamedJoin = streamedBandsDF.join(streamedGuitaristDF, streamedGuitaristDF.col("band") === streamedBandsDF.col("id"))

    /*
    * - inner joins are supported
    * - left/right outer joins are supported, but MUST have watermarks
    * - full outer joins are NOT supported
    * */
    streamedJoin.writeStream
      .format("console")
      .outputMode("append") // only append supported for stream vs stream join
      .start()
      .awaitTermination()
  }

  def main(args: Array[String]): Unit = {
    joinStreamWithStream()
  }
}

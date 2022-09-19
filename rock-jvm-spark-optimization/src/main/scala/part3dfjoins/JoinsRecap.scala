package part3dfjoins

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{Column, DataFrame, SparkSession}

object JoinsRecap {
  val spark: SparkSession = SparkSession.builder()
    .master("local[*]")
    .appName("Joins Recap")
    .getOrCreate()

  spark.sparkContext.setLogLevel("ERROR")

  val sc: SparkContext = spark.sparkContext

  val guitarsDF: DataFrame = spark.read
    .option("inferSchema", "true")
    .json("src/main/resources/data/guitars")

  val guitaristsDF: DataFrame = spark.read
    .option("inferSchema", "true")
    .json("src/main/resources/data/guitarPlayers")

  val bandsDF: DataFrame = spark.read
    .option("inferSchema", "true")
    .json("src/main/resources/data/bands")

  // inner joins
  val joinCondition: Column = guitaristsDF.col("band") === bandsDF.col("id")
  val guitaristsBandsDF: DataFrame = guitaristsDF.join(bandsDF, joinCondition, "inner")

  // outer joins
  // left
  guitaristsDF.join(bandsDF, joinCondition, "left_outer")
  // right
  guitaristsDF.join(bandsDF, joinCondition, "right_outer")
  // full
  guitaristsDF.join(bandsDF, joinCondition, "outer")

  // semi joins = everything in the left DF for which THERE IS a row in the right DF satisfying the condition
  // essentially a filter
  guitaristsDF.join(bandsDF, joinCondition, "left_semi")

  // anti join = everything in the left DF for which THERE IS NOT a row in the right DF satisfying the condition
  // also a filter
  guitaristsDF.join(bandsDF, joinCondition, "left_anti")

  // cross join = everything in the left DF with everything in the right DF
  // DANGEROUS: CARTESIAN PRODUCT
  // careful with outer joins with non-unique keys

  // RDD joins
  val colorScores: Seq[(String, Int)] = Seq(
    ("blue",1),
    ("red",5),
    ("green",5),
    ("yellow",0),
    ("cyan",6)
  )

  val colorsRDD: RDD[(String, Int)] = sc.parallelize(colorScores)
  val text = "The sky is blue, but the orange pale sun turns from yellow to red"
  val words: Array[(String, Int)] = text.split(" ").map(_.toUpperCase()).map((_, 1)) // standard technique for counting words with RDDs
  val wordsRDD: RDD[(String, Int)] = sc.parallelize(words).reduceByKey(_ + _) // counting word occurrence
  val scores: RDD[(String, (Int, Int))] = wordsRDD.join(colorsRDD) // implied join type is inner

  def main(args: Array[String]): Unit = {

  }
}

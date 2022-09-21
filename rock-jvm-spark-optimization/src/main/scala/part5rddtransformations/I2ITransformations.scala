package part5rddtransformations

import generator.DataGenerator
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession

import scala.collection.mutable

object I2ITransformations {
  val spark: SparkSession = SparkSession.builder()
    .master("local[*]")
    .appName("I2I Transformations")
    .getOrCreate()

  spark.sparkContext.setLogLevel("ERROR")
  val sc: SparkContext = spark.sparkContext

  /*
  * Science Project
  * each metric has identifier, value
  *
  * Return the smallest("best") 10 metrics with (identifiers + values)
  * */

  val limit = 10
  def readMetrics(): RDD[(String, Double)] = sc.textFile("src/main/resources/generated/metrics/10m.txt")
    .map { line =>
      val tokens = line.split(" ")
      val name = tokens(0)
      val value = tokens(1)

      (name, value.toDouble)
    }

  def printTopMetrics() = {
    val sortedMetrics = readMetrics().sortBy(_._2).take(limit)
    sortedMetrics.foreach(println)
  }

  def printTopMetricsI2I() = {
    val iteratorToIteratorTransformation = (records: Iterator[(String, Double)]) => {
      // i2i transformations
      /*
      * Benefits:
      * - they are NARROW TRANSFORMATIONS
      * - Spark will "selectively" spill data to disk when partitions are too big for memory
      *
      * Warning: don't traverse more than once or convert to collection
      * */
    implicit val ordering: Ordering[(String, Double)] = Ordering.by[(String, Double), Double](_._2)
      val limitedCollection = new mutable.TreeSet[(String, Double)]()

      records.foreach { record =>
        limitedCollection.add(record)
        if (limitedCollection.size > limit)
          limitedCollection.remove(limitedCollection.last)
      }

      // I've traversed the iterator
      limitedCollection.toIterator
    }
    val topMetrics = readMetrics()
      .mapPartitions(iteratorToIteratorTransformation)
      .repartition(1)
      .mapPartitions(iteratorToIteratorTransformation)

    val result = topMetrics.take(limit)
    result.foreach(println)
  }

  def main(args: Array[String]): Unit = {
//    DataGenerator.generateMetrics("src/main/resources/generated/metrics/10m.txt", 10000000)
    printTopMetrics()
    printTopMetricsI2I()
    Thread.sleep(10000000)
  }
}

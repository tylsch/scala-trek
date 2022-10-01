package part4partitioning

import org.apache.spark.sql.SparkSession

object RepartitionCoalesce {
  val spark = SparkSession.builder()
    .appName("Repartition and Coalesce")
    .master("local[*]")
    .getOrCreate()

  spark.sparkContext.setLogLevel("ERROR")
  val sc = spark.sparkContext

  val numbers = sc.parallelize(1 to 10000000)
  println(numbers.partitions.length) // number of virtual cores

  // repartition
  val repartitionedNumbers = numbers.repartition(2)
  repartitionedNumbers.count()

  // coalesce - fundamentally different
  val coalescedNumbers = numbers.coalesce(2) // for a smaller number of partitions
  coalescedNumbers.count()

  // force coalesce to be a shuffle
  val forcedShuffleNumbers = numbers.coalesce(2, shuffle = true) // force a shuffle

  def main(args: Array[String]): Unit = {
    Thread.sleep(1000000)
  }
}

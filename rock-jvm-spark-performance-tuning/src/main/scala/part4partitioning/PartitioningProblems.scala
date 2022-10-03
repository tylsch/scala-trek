package part4partitioning

import org.apache.spark.sql.SparkSession
import org.apache.spark.util.SizeEstimator

object PartitioningProblems {
  val spark = SparkSession.builder()
    .appName("Partitioning Problems")
    .master("local[*]")
    .getOrCreate()

  spark.sparkContext.setLogLevel("ERROR")

  def processNumbers(nPartitions: Int) = {
    val numbers = spark.range(100000000) // ~800MB
    val repartitionNumbers = numbers.repartition(nPartitions)
    repartitionNumbers.cache()
    repartitionNumbers.count()

    // the computation I care about
    repartitionNumbers.selectExpr("sum(id)").show()
  }

  // 1 - use size estimator
  def dfSizeEstimator() = {
    val numbers = spark.range(100000)
    println(SizeEstimator.estimate(numbers)) // usually works, not super accurate, within an order of magnitude
    // larger number because it measures the actual JVM object of the dataset
    numbers.cache()
    numbers.count()
  }

  // 2 - use query plan
  def estimateWithQueryPlan() = {
    val numbers = spark.range(100000)
    println(numbers.queryExecution.optimizedPlan.stats.sizeInBytes) // accurate size in bytes for the Data
  }

  def estimateRDD() = {
    val numbers = spark.sparkContext.parallelize(1 to 100000)
    numbers.cache().count()
  }

  def main(args: Array[String]): Unit = {
//    processNumbers(2)
//    processNumbers(20)
//    processNumbers(200)
//    processNumbers(2000)
//    processNumbers(20000)
//    dfSizeEstimator()
//    estimateWithQueryPlan()
    estimateRDD()

    Thread.sleep(10000000)
  }
}

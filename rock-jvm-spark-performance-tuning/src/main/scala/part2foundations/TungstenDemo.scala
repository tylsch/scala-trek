package part2foundations

import org.apache.spark.sql.SparkSession

object TungstenDemo {
  val spark = SparkSession.builder()
    .appName("Tungsten Demo")
    .master("local[*]")
    .getOrCreate()

  spark.sparkContext.setLogLevel("ERROR")

  val sc = spark.sparkContext

  val numbersRDD = sc.parallelize(1 to 10000000).cache()
  numbersRDD.count()
  numbersRDD.count() // much faster

  import spark.implicits._
  val numbersDF = numbersRDD.toDF("value").cache() // cached with Tungsten
  numbersDF.count()
  numbersDF.count() // much faster

  // Tungsten is active in WholeStageCodegen
  /*
  * == Physical Plan ==
  HashAggregate(keys=[], functions=[sum(id#54L)])
  +- Exchange SinglePartition, true, [id=#68]
     +- HashAggregate(keys=[], functions=[partial_sum(id#54L)])
        +- Range (0, 10000000, step=1, splits=16)
  * */
  spark.conf.set("spark.sql.codegen.wholeStage", "false")
  val noWholeStageSum = spark.range(10000000).selectExpr("sum(id)")
  noWholeStageSum.explain()
  noWholeStageSum.show()

  /*
  * == Physical Plan ==
  *(2) HashAggregate(keys=[], functions=[sum(id#66L)])
  +- Exchange SinglePartition, true, [id=#99]
     +- *(1) HashAggregate(keys=[], functions=[partial_sum(id#66L)])
        +- *(1) Range (0, 10000000, step=1, splits=16)
  * */

  spark.conf.set("spark.sql.codegen.wholeStage", "true")
  val wholeStageSum = spark.range(10000000).selectExpr("sum(id)")
  wholeStageSum.explain()
  wholeStageSum.show()

  def main(args: Array[String]): Unit = {
    Thread.sleep(10000000)
  }
}

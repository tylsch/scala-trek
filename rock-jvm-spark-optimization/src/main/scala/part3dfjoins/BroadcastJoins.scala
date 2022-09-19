package part3dfjoins

import org.apache.spark.SparkContext
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.{IntegerType, StringType, StructField, StructType}
import org.apache.spark.sql.{Row, SparkSession}

object BroadcastJoins {
  val spark: SparkSession = SparkSession.builder()
    .master("local[*]")
    .appName("Broadcast Joins")
    .getOrCreate()

  spark.sparkContext.setLogLevel("ERROR")

  val sc: SparkContext = spark.sparkContext

  val rows = sc.parallelize(List(
    Row(0, "zero"),
    Row(1, "first"),
    Row(2, "second"),
    Row(3, "third")
  ))

  val rowsSchema = StructType(Array(
    StructField("id", IntegerType),
    StructField("order", StringType)
  ))

  val lookupTable = spark.createDataFrame(rows, rowsSchema)

  val table = spark.range(1, 100000000)

  val joined = table.join(lookupTable, "id")
  joined.explain()
//  joined.show()

  // smarter join
  val joinedSmart = table.join(broadcast(lookupTable), "id")
  joinedSmart.explain()
//  joinedSmart.show()

  // auto-broadcast join
  spark.conf.set("spark.sql.autoBroadcastJoinedThreshold", 30)

  val bigTable = spark.range(1, 100000000)
  val smallTable = spark.range(1,10000) // size estimated by Spark - auto-broadcast
  val joinedNumbers = bigTable.join(smallTable, "id")
  joinedNumbers.explain()


  def main(args: Array[String]): Unit = {
    Thread.sleep(10000000)
  }
}

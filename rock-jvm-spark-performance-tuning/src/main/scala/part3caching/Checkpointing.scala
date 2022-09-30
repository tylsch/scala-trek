package part3caching

import org.apache.spark.sql.SparkSession
import org.apache.spark.storage.StorageLevel
import part3caching.Caching.spark

object Checkpointing {
  val spark = SparkSession.builder()
    .appName("Checkpointing")
    .master("local[*]")
    .getOrCreate()

  spark.sparkContext.setLogLevel("ERROR")
  val sc = spark.sparkContext

  def dempCheckpoint() = {
    val flightsDF = spark.read
      .option("inferSchema", "true")
      .json("src/main/resources/data/flights")

    // do some expensive computation
    val orderedFlights = flightsDF.orderBy("dist")

    // checkpointing is used to avoid failure in computations
    // needs to be configured
    sc.setCheckpointDir("spark-warehouse")

    // checkpoint a DF = save the DF to disk
    val checkpointedFlights = orderedFlights.checkpoint() // an action

    // query plan difference with checkpointed DF

    /*
    * == Physical Plan ==
    *(1) Sort [dist#16 ASC NULLS FIRST], true, 0
    +- Exchange rangepartitioning(dist#16 ASC NULLS FIRST, 200), true, [id=#10]
       +- FileScan json [_id#7,arrdelay#8,carrier#9,crsarrtime#10L,crsdephour#11L,crsdeptime#12L,crselapsedtime#13,depdelay#14,dest#15,dist#16,dofW#17L,origin#18] Batched: false, DataFilters: [], Format: JSON, Location: InMemoryFileIndex[file:/home/tylsch/Repos/Scala/scala-trek/rock-jvm-spark-performance-tuning/src/..., PartitionFilters: [], PushedFilters: [], ReadSchema: struct<_id:string,arrdelay:double,carrier:string,crsarrtime:bigint,crsdephour:bigint,crsdeptime:b...
    * */
    orderedFlights.explain()
    /*
    *
    == Physical Plan ==
    *(1) Scan ExistingRDD[_id#7,arrdelay#8,carrier#9,crsarrtime#10L,crsdephour#11L,crsdeptime#12L,crselapsedtime#13,depdelay#14,dest#15,dist#16,dofW#17L,origin#18]
    * */
    checkpointedFlights.explain()

    checkpointedFlights.show()
  }

  def cachingJobRDD() = {
    val numbers = sc.parallelize(1 to 10000000)
    val descNumbers = numbers.sortBy(-_).persist(StorageLevel.DISK_ONLY)
    descNumbers.sum()
    descNumbers.sum()
  }

  def checkpointingJobRDD() = {
    sc.setCheckpointDir("spark-warehouse")
    val numbers = sc.parallelize(1 to 10000000)
    val descNumbers = numbers.sortBy(-_)
    descNumbers.checkpoint()
    descNumbers.sum()
    descNumbers.sum()
  }

  def cachingJobDF() = {
    val flightsDF = spark.read
      .option("inferSchema", "true")
      .json("src/main/resources/data/flights")

    val orderedFlights = flightsDF.orderBy("dist")
    orderedFlights.persist(StorageLevel.DISK_ONLY)
    orderedFlights.count()
    orderedFlights.count()
  }

  def checkpointingJobDF() = {
    sc.setCheckpointDir("spark-warehouse")
    val flightsDF = spark.read
      .option("inferSchema", "true")
      .json("src/main/resources/data/flights")

    val orderedFlights = flightsDF.orderBy("dist")
    val checkpointedFlights = flightsDF.checkpoint()
    checkpointedFlights.count()
    checkpointedFlights.count()
  }

  def main(args: Array[String]): Unit = {
//    cachingJobRDD()
//    checkpointingJobRDD()
    cachingJobDF()
    checkpointingJobDF()
    Thread.sleep(10000000)
  }
}

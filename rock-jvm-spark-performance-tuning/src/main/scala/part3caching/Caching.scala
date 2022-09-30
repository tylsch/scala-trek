package part3caching

import org.apache.spark.sql.SparkSession
import org.apache.spark.storage.StorageLevel

object Caching {
  val spark = SparkSession.builder()
    .appName("Caching")
    .config("spark.memory.offHeap.enabled", "true")
    .config("spark.memory.offHeap.size", 100000000)
    .master("local[*]")
    .getOrCreate()

  spark.sparkContext.setLogLevel("ERROR")

  val flighsDF = spark.read
    .option("inferSchema", "true")
    .json("src/main/resources/data/flights")

  flighsDF.count()

  // simulate an "expensive" operation
  val orderedFlightsDF = flighsDF.orderBy("dist")

  // scenario: use this DF  multiple times
  orderedFlightsDF.persist(
    // no argument: MEMORY_AND_DISK
    //StorageLevel.MEMORY_ONLY // cache the DF in memory EXACTLY - CPU efficient. memory expensive
    //StorageLevel.DISK_ONLY // cache the DF to DISK - CPU and memory efficient, and slower
    //StorageLevel.MEMORY_AND_DISK // cache this DF to both the heap AND the disk - first caches to memory, but if DF is EVICTED then written to disk

    // Modifiers
    //StorageLevel.MEMORY_ONLY_SER // memory only serialized - more CPU intensive, memory saving - more impactful for RDDs
    //StorageLevel.MEMORY_ONLY_2 // memory only, replicated twice - for resiliency, 2x memory usage
    //StorageLevel.MEMORY_ONLY_SER_2 // memory only serialized, 2x replicated

    // off-heap
    StorageLevel.OFF_HEAP // cache outside the JVM, done with Tungsten, still stored on machine RAM, needs to be configured, CPU and Memory efficient
  )


  /*
  * Without cache: sorted count ~0.1s
  * */

  orderedFlightsDF.count()
  orderedFlightsDF.count()

  // remove from cache
  orderedFlightsDF.unpersist() // removes from cache

  // change cache name
  orderedFlightsDF.createOrReplaceTempView("orderedFlights")
  spark.catalog.cacheTable("orderedFlights")
  orderedFlightsDF.count()

  // RDDs
  val flightsRDD = orderedFlightsDF.rdd
  flightsRDD.persist(StorageLevel.MEMORY_ONLY_SER)
  flightsRDD.count()

  def main(args: Array[String]): Unit = {
    Thread.sleep(10000000)
  }

}

package part3dfjoins

import org.apache.spark.SparkContext
import org.apache.spark.sql.functions._
import org.apache.spark.sql.{DataFrame, SparkSession}

object ColumnPruning {
  val spark: SparkSession = SparkSession.builder()
    .master("local[*]")
    .appName("Column Pruning")
    .getOrCreate()

  spark.sparkContext.setLogLevel("ERROR")
  val sc: SparkContext = spark.sparkContext
  import spark.implicits._

  val guitarsDF: DataFrame = spark.read
    .option("inferSchema", "true")
    .json("src/main/resources/data/guitars")

  val guitaristsDF: DataFrame = spark.read
    .option("inferSchema", "true")
    .json("src/main/resources/data/guitarPlayers")

  val bandsDF: DataFrame = spark.read
    .option("inferSchema", "true")
    .json("src/main/resources/data/bands")

  val joinCondition = guitaristsDF.col("band") === bandsDF.col("id")
  val guitaristsBandDF = guitaristsDF.join(bandsDF, joinCondition)
  guitaristsBandDF.explain()

  /*
  * == Physical Plan ==
  *(2) BroadcastHashJoin [band#22L], [id#38L], Inner, BuildLeft
  :- BroadcastExchange HashedRelationBroadcastMode(List(input[0, bigint, true])), [id=#34]
  :  +- *(1) Project [band#22L, guitars#23, id#24L, name#25]
  :     +- *(1) Filter isnotnull(band#22L)
  :        +- FileScan json [band#22L,guitars#23,id#24L,name#25] Batched: false, DataFilters: [isnotnull(band#22L)], Format: JSON, Location: InMemoryFileIndex[file:/home/tylsch/Repos/Scala/scala-trek/rock-jvm-spark-optimization/src/main/r..., PartitionFilters: [], PushedFilters: [IsNotNull(band)], ReadSchema: struct<band:bigint,guitars:array<bigint>,id:bigint,name:string>
  +- *(2) Project [hometown#37, id#38L, name#39, year#40L]
     +- *(2) Filter isnotnull(id#38L)
        +- FileScan json [hometown#37,id#38L,name#39,year#40L] Batched: false, DataFilters: [isnotnull(id#38L)], Format: JSON, Location: InMemoryFileIndex[file:/home/tylsch/Repos/Scala/scala-trek/rock-jvm-spark-optimization/src/main/r..., PartitionFilters: [], PushedFilters: [IsNotNull(id)], ReadSchema: struct<hometown:string,id:bigint,name:string,year:bigint>
  * */

  val guitaristsWithoutBands = guitaristsDF.join(bandsDF, joinCondition, "left_anti")
  guitaristsWithoutBands.explain()

  /*
  * == Physical Plan ==
  *(2) BroadcastHashJoin [band#22L], [id#38L], LeftAnti, BuildRight
  :- FileScan json [band#22L,guitars#23,id#24L,name#25] Batched: false, DataFilters: [], Format: JSON, Location: InMemoryFileIndex[file:/home/tylsch/Repos/Scala/scala-trek/rock-jvm-spark-optimization/src/main/r..., PartitionFilters: [], PushedFilters: [], ReadSchema: struct<band:bigint,guitars:array<bigint>,id:bigint,name:string>
  +- BroadcastExchange HashedRelationBroadcastMode(List(input[0, bigint, true])), [id=#63]
     +- *(1) Project [id#38L] <- COLUMN PRUNING
        +- *(1) Filter isnotnull(id#38L)
           +- FileScan json [id#38L] Batched: false, DataFilters: [isnotnull(id#38L)], Format: JSON, Location: InMemoryFileIndex[file:/home/tylsch/Repos/Scala/scala-trek/rock-jvm-spark-optimization/src/main/r..., PartitionFilters: [], PushedFilters: [IsNotNull(id)], ReadSchema: struct<id:bigint>
  * Column Pruning = cut off columns that are not relevant = shrinks DF
  * !!! useful for joins and groups
  *  */

  // project and filter push-down
  val namesDF = guitaristsBandDF.select(guitaristsDF.col("name"), bandsDF.col("name"))
  namesDF.explain()

  /*
  * == Physical Plan ==
  *(2) Project [name#25, name#39]
  +- *(2) BroadcastHashJoin [band#22L], [id#38L], Inner, BuildRight
     :- *(2) Project [band#22L, name#25] <- COLUMN PRUNING
     :  +- *(2) Filter isnotnull(band#22L)
     :     +- FileScan json [band#22L,name#25] Batched: false, DataFilters: [isnotnull(band#22L)], Format: JSON, Location: InMemoryFileIndex[file:/home/tylsch/Repos/Scala/scala-trek/rock-jvm-spark-optimization/src/main/r..., PartitionFilters: [], PushedFilters: [IsNotNull(band)], ReadSchema: struct<band:bigint,name:string>
     +- BroadcastExchange HashedRelationBroadcastMode(List(input[0, bigint, true])), [id=#100]
        +- *(1) Project [id#38L, name#39]
           +- *(1) Filter isnotnull(id#38L)
              +- FileScan json [id#38L,name#39] Batched: false, DataFilters: [isnotnull(id#38L)], Format: JSON, Location: InMemoryFileIndex[file:/home/tylsch/Repos/Scala/scala-trek/rock-jvm-spark-optimization/src/main/r..., PartitionFilters: [], PushedFilters: [IsNotNull(id)], ReadSchema: struct<id:bigint,name:string>
  * Spark tends to drop columns as early as possible
  * Should be YOUR goal as well
  *  */

  val rockDF = guitaristsDF
    .join(bandsDF, joinCondition)
    .join(guitarsDF, array_contains(guitaristsDF.col("guitars"), guitarsDF.col("id")))

  val essentialsDF = rockDF.select(guitaristsDF.col("name"), bandsDF.col("name"), upper(guitarsDF.col("make")))
  essentialsDF.explain()

  /*
  * == Physical Plan ==
  *(3) Project [name#25, name#39, upper(make#9) AS upper(make)#147]
  +- BroadcastNestedLoopJoin BuildRight, Inner, array_contains(guitars#23, id#8L)
     :- *(2) Project [guitars#23, name#25, name#39]
     :  +- *(2) BroadcastHashJoin [band#22L], [id#38L], Inner, BuildRight
     :     :- *(2) Project [band#22L, guitars#23, name#25]
     :     :  +- *(2) Filter isnotnull(band#22L)
     :     :     +- FileScan json [band#22L,guitars#23,name#25] Batched: false, DataFilters: [isnotnull(band#22L)], Format: JSON, Location: InMemoryFileIndex[file:/home/tylsch/Repos/Scala/scala-trek/rock-jvm-spark-optimization/src/main/r..., PartitionFilters: [], PushedFilters: [IsNotNull(band)], ReadSchema: struct<band:bigint,guitars:array<bigint>,name:string>
     :     +- BroadcastExchange HashedRelationBroadcastMode(List(input[0, bigint, true])), [id=#154]
     :        +- *(1) Project [id#38L, name#39]
     :           +- *(1) Filter isnotnull(id#38L)
     :              +- FileScan json [id#38L,name#39] Batched: false, DataFilters: [isnotnull(id#38L)], Format: JSON, Location: InMemoryFileIndex[file:/home/tylsch/Repos/Scala/scala-trek/rock-jvm-spark-optimization/src/main/r..., PartitionFilters: [], PushedFilters: [IsNotNull(id)], ReadSchema: struct<id:bigint,name:string>
     +- BroadcastExchange IdentityBroadcastMode, [id=#144]
        +- FileScan json [id#8L,make#9] Batched: false, DataFilters: [], Format: JSON, Location: InMemoryFileIndex[file:/home/tylsch/Repos/Scala/scala-trek/rock-jvm-spark-optimization/src/main/r..., PartitionFilters: [], PushedFilters: [], ReadSchema: struct<id:bigint,make:string>

  * LESSON: if you anticipate that the joined table is much larger than the table on whose column you are applying the map-side operation, e.g. " * 5" or "upper", do this operation on the small table FIRST
  *
  * Particularly useful for OUTER joins
  * */


  def main(args: Array[String]): Unit = {

  }
}

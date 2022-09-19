package part6advanced

import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming.{GroupState, GroupStateTimeout}
import org.apache.spark.sql.{Dataset, SparkSession}

object StatefulComputations {
  val spark: SparkSession = SparkSession.builder()
    .appName("Stateful Computation")
    .master("local[2]")
    .getOrCreate()

  spark.sparkContext.setLogLevel("ERROR")

  import spark.implicits._

  case class SocialPostRecord(postType: String, count: Int, storageUsed: Int)
  case class SocialPostBulk(postType: String, count: Int, totalStorageUsed: Int)
  case class AveragePostStorage(postType: String, averageStorage: Double)

  def readSocialUpdates(): Dataset[SocialPostRecord] =
    spark.readStream
      .format("socket")
      .option("host", "localhost")
      .option("port", 12345)
      .load()
      .as[String]
      .map { line =>
        val tokens = line.split(",")
        SocialPostRecord(tokens(0), tokens(1).toInt, tokens(2).toInt)
      }

  def updateAverageStorage(
                            postType: String, // the key by which the grouping was made
                            group: Iterator[SocialPostRecord], // a batch of data associated to the key
                            state: GroupState[SocialPostBulk] // like an "option", I have to manage manually
                          ): AveragePostStorage = { // a single value that I will output per the entire group
    /*
    * - extract the state to start with
    * - for all the items in the group
    *   - aggregate data: summing up the total count and total storage
    * - update the state with the aggregated data
    * - return a single value of type AveragePostStorage
    * */

    // extract state
    val previousBulk: SocialPostBulk =
      if (state.exists) state.get
      else SocialPostBulk(postType, 0, 0)

    // iterate through the group
    val totalAggregateData: (Int, Int) = group.foldLeft((0,0)) { (currentData, record) =>
      val (currentCount, currentStorage) = currentData
      (currentCount + record.count, currentStorage + record.storageUsed)
    }

    // update the state with new aggregated data
    val (totalCount, totalStorage) = totalAggregateData
    val newPostBulk = SocialPostBulk(postType, previousBulk.count + totalCount, previousBulk.totalStorageUsed + totalStorage)
    state.update(newPostBulk)

    // return a single output value
    AveragePostStorage(postType = postType, averageStorage = newPostBulk.totalStorageUsed * 1.0 / newPostBulk.count)
  }

  def getAveragePostStorage() = {
    val socialStream = readSocialUpdates()

    val regularSqlAvgByPostType = socialStream
      .groupByKey(_.postType)
      .agg(sum(col("count")).as("totalCount").as[Int], sum(col("storageUsed")).as("totalStorage").as[Int])
      .selectExpr("key as postType", "totalStorage/totalCount as avgStorage")

    val averageByPostType = socialStream
      .groupByKey(_.postType)
      .mapGroupsWithState(GroupStateTimeout.NoTimeout())(updateAverageStorage)

    averageByPostType
      .writeStream
      .format("console")
      .outputMode("update") // append not supported on mapGroupWithState
      .start()
      .awaitTermination()
  }

  def main(args: Array[String]): Unit = {
    getAveragePostStorage()
  }
}
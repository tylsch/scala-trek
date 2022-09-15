package part7science

import org.apache.spark.sql.streaming.{GroupState, GroupStateTimeout, OutputMode}
import org.apache.spark.sql.{Dataset, SparkSession}

object ScienceSparkAggregator {


  val spark: SparkSession = SparkSession.builder()
    .appName("The Science project")
    .master("local[2]")
    .getOrCreate()

  spark.sparkContext.setLogLevel("ERROR")

  import spark.implicits._
  case class UserResponse(sessionId: String, clickDuration: Long)
  case class UserAverageResponse(sessionId: String, averageDuration: Double)

  def readUserResponses(): Dataset[UserResponse] = spark.readStream
    .format("kafka")
    .option("kafka.bootstrap.servers", "localhost:9092")
    .option("subscribe", "science")
    .load()
    .select("value")
    .as[String]
    .map { line =>
      val tokens = line.split(",")
      val sessionId = tokens(0)
      val duration = tokens(1).toLong

      UserResponse(sessionId, duration)
    }

  /*
  * Aggregate the ROLLING average response time over the past 3 clicks
  * 1 2 3 4 5 6 7 8 9 10
  * 6 9 12 15 18 21 24 27
  * */

  def updateUserResponseTime(n: Int)(sessionId: String, group: Iterator[UserResponse], state: GroupState[List[UserResponse]]): Iterator[UserAverageResponse] = {
    group.flatMap { record =>
      val lastWindow = if (state.exists) state.get else List()
      val windowLength = lastWindow.length
      val newWindow = if (windowLength >= n) lastWindow.tail :+ record else lastWindow :+ record

      // For Spark to give us access to the state in the next patch
      state.update(newWindow)
      if (newWindow.length >= n) {
        val newAverage = newWindow.map(_.clickDuration).sum * 1.0 / n
        Iterator(UserAverageResponse(sessionId = sessionId, averageDuration = newAverage))
      }
      else
        Iterator()
    }
  }

  def getAverageResponseTime(n: Int): Unit = {
    readUserResponses()
      .groupByKey(_.sessionId)
      .flatMapGroupsWithState(OutputMode.Append(), GroupStateTimeout.NoTimeout())(updateUserResponseTime(n))
      .writeStream
      .format("console")
      .outputMode("append")
      .start()
      .awaitTermination()
  }


  def logUserResponses(): Unit = {
    readUserResponses()
      .writeStream
      .format("console")
      .outputMode("append")
      .start()
      .awaitTermination()
  }

  def main(args: Array[String]): Unit = {
    getAverageResponseTime(3)
  }
}

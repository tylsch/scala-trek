package part5twitter

import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.{Seconds, StreamingContext}
import twitter4j.Status

object TwitterProject {
  val spark: SparkSession = SparkSession.builder()
    .appName("Twitter Project")
    .master("local[*]")
    .getOrCreate()

  val ssc = new StreamingContext(spark.sparkContext, Seconds(1))

  def readTwitter(): Unit = {
    val twitterStream: DStream[Status] = ssc.receiverStream(new TwitterReceiver)
    val tweets: DStream[String] = twitterStream.map { status =>
      val username = status.getUser.getName
      val followers = status.getUser.getFollowersCount
      val text = status.getText
      val sentiment = SentimentAnalysis.detectSentiment(text)

      s"User $username ($followers followers) says $sentiment: $text"
    }

    tweets.print()
    ssc.start()
    ssc.awaitTermination()
  }

  def getAverageTweetLength: DStream[Double] = {
    val tweets = ssc.receiverStream(new TwitterReceiver)
    tweets
      .map(status => status.getText)
      .map(text => text.length)
      .map(length => (length, 1))
      .reduceByWindow((tuple1, tuple2) => (tuple1._1 + tuple2._1, tuple1._2 + tuple2._2), Seconds(5), Seconds(5))
      .map { megaTuple =>
        val tweetLengthSum = megaTuple._1
        val tweetCount = megaTuple._2

        tweetLengthSum * 1.0 / tweetCount
      }
  }

  def computeMostPopularHashtags(): DStream[(String, Int)] = {
    val tweets = ssc.receiverStream(new TwitterReceiver)
    tweets
      .flatMap(_.getText.split(" "))
      .filter(_.startsWith("#"))
      .map(hashtag => (hashtag, 1))
      .reduceByKeyAndWindow((x, y) => x + y, (x, y) => x - y, Seconds(60), Seconds(10))
      .transform(rdd => rdd.sortBy(tuple => tuple._2))
  }

  def main(args: Array[String]): Unit = {
    readTwitter()
  }
}

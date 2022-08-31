package part3lowlevel

import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.dstream.{DStream, ReceiverInputDStream}
import org.apache.spark.streaming.{Minutes, Seconds, StreamingContext}

object DStreamsWindowTransformations {
  val spark: SparkSession = SparkSession.builder()
    .appName("DStreams Window Transformations")
    .master("local[2]")
    .getOrCreate()

  val ssc = new StreamingContext(sparkContext = spark.sparkContext, batchDuration = Seconds(1))

  def readLines(): ReceiverInputDStream[String] = ssc.socketTextStream("localhost", 12345)

  /*
    * window = keep all the values emitted between now and X time back
    * window interval updated with every batch
    * window interval must be a multiple of the batch interval
    * */
  def linesByWindow(): DStream[String] = readLines().window(Seconds(10))

  def linesBySlidingWindow(): DStream[String] = readLines().window(Seconds(10), Seconds(5))

  // count the number of elements over a window
  def countLinesByWindow(): DStream[Long] = readLines().countByWindow(Minutes(60), Seconds(30))

  // aggregate window in a different way over a window
  def sumAllTextByWindow(): DStream[Int] = readLines().map(_.length).window(Seconds(10), Seconds(5)).reduce(_ + _)

  def sumAllTextByWindowAlt(): DStream[Int] = readLines().map(_.length).reduceByWindow(_ + _, Seconds(10), Seconds(5))

  // tumbling window
  def linesByTumblingWindow(): DStream[String] = readLines().window(Seconds(10), Seconds(10))

  def computeWordOccurrencesByWindow(): DStream[(String, Int)] = {
    ssc.checkpoint("src/main/resources/data/checkpoints")

    readLines()
      .flatMap(_.split(" "))
      .map(word => (word, 1))
      .reduceByKeyAndWindow(
        (a, b) => a + b, // reduction function
        (a, b) => a - b, // "inverse" function
        Seconds(60), // window duration
        Seconds(30) // sliding duration
      )
  }

  /*
  * Exercise:
  * Word longer than 5 chars => $2
  * every other word => $0
  *
  * Input text into the terminal => money made over the past 30 seconds, updated every 10 seconds
  * */

  val moneyPerExpenseWord = 2
  def showMeTheMoney(): DStream[Int] =
    readLines()
      .flatMap(line => line.split(" "))
      .filter(_.length < 5)
      .map(_ => moneyPerExpenseWord)
      .reduce(_ + _)
      .window(Seconds(30), Seconds(10))
      .reduce(_ + _)

  def showMeTheMoney2(): DStream[Int] =
    readLines()
      .flatMap(line => line.split(" "))
      .filter(_.length < 5)
      .countByWindow(Seconds(30), Seconds(10))
      .map(_.toInt * moneyPerExpenseWord)

  def showMeTheMoney3(): DStream[Int] =
    readLines()
      .flatMap(line => line.split(" "))
      .filter(_.length < 5)
      .map(_ => moneyPerExpenseWord)
      .reduceByWindow(_ + _, Seconds(30), Seconds(10))

  def showMeTheMoney4(): DStream[(String, Int)] = {
    ssc.checkpoint("src/main/resources/data/checkpoints")

    readLines()
      .flatMap(line => line.split(" "))
      .filter(_.length < 5)
      .map { word =>
        if (word.length >= 5) ("expensive", 2)
        else ("cheap", 0)
      }
      .reduceByKeyAndWindow(_ + _, _ - _, Seconds(30), Seconds(10))
  }


  def main(args: Array[String]): Unit = {
    computeWordOccurrencesByWindow().print()
    ssc.start()
    ssc.awaitTermination()
  }
}

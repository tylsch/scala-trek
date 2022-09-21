package part5rddtransformations

import com.univocity.parsers.common.record.Record
import generator.DataGenerator
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession

object ReusingObjects {
  val spark: SparkSession = SparkSession.builder()
    .master("local[*]")
    .appName("Reusing JVM objects")
    .getOrCreate()

  spark.sparkContext.setLogLevel("ERROR")
  val sc: SparkContext = spark.sparkContext

  /*
  * Analyze Text
  * Receive batches of text from data sources
  * "35 // some text"
  *
  * Stats per each data source id:
  * - the number of lines in total
  * - total number of words in total
  * - length of the longest word
  * - the number of occurrences of the word "imperdiet"
  *
  * Results should be VERY FAST!
  * */

  val textPath = "src/main/resources/generated/lipsum/3m.txt"
  def generateData() = {
    DataGenerator.generateText(textPath, 60000000, 3000000, 200)
  }

  val text = sc.textFile(textPath).map { line =>
    val tokens = line.split("//")
    (tokens(0), tokens(1))
  }

  // Version 1
  case class TextStats(nLines: Int, nWords: Int, maxWordLength: Int, occurrences: Int)
  object TextStats {
    val zero = TextStats(0,0,0,0)
  }

  def collectStats(): collection.Map[String, TextStats] = {

    def aggregateNewRecord(textStats: TextStats, record: String): TextStats = {
      val newWords = record.split(" ")
      val longestWord = newWords.maxBy(_.length)
      val newOccurrences = newWords.count(_ == "imperdiet")
      TextStats(
        textStats.nLines + 1,
        textStats.nWords + newWords.length,
        if (longestWord.length > textStats.maxWordLength) longestWord.length else textStats.maxWordLength,
        textStats.occurrences + newOccurrences
      )
    }

    def combineStats(stats: TextStats, stats2: TextStats): TextStats = {
      TextStats(
        stats.nLines + stats2.nLines,
        stats.nWords + stats.nWords,
        if (stats.maxWordLength > stats2.maxWordLength) stats.maxWordLength else stats2.maxWordLength,
        stats.occurrences + stats2.occurrences
      )
    }

    val aggregate: RDD[(String, TextStats)] = text.aggregateByKey(TextStats.zero)(aggregateNewRecord, combineStats)
    aggregate.collectAsMap()
  }

  // Version 2
  class MutableTextStats(var nLines: Int, var nWords: Int, var maxWordLength: Int, var occurrences: Int) extends Serializable
  object MutableTextStats extends Serializable {
    def zero = new MutableTextStats(0,0,0,0)
  }

  def collectStats2(): collection.Map[String, MutableTextStats] = {

    def aggregateNewRecord(textStats: MutableTextStats, record: String): MutableTextStats = {
      val newWords = record.split(" ")
      val longestWord = newWords.maxBy(_.length)
      val newOccurrences = newWords.count(_ == "imperdiet")

      textStats.nLines += 1
      textStats.nWords += newWords.length
      textStats.maxWordLength = if (longestWord.length > textStats.maxWordLength) longestWord.length else textStats.maxWordLength
      textStats.occurrences += newOccurrences
      textStats
    }

    def combineStats(stats: MutableTextStats, stats2: MutableTextStats): MutableTextStats = {
      stats.nLines += stats2.nLines
      stats.nWords += stats.nWords
      stats.maxWordLength = if (stats.maxWordLength > stats2.maxWordLength) stats.maxWordLength else stats2.maxWordLength
      stats.occurrences += stats2.occurrences
      stats
    }

    val aggregate: RDD[(String, MutableTextStats)] = text.aggregateByKey(MutableTextStats.zero)(aggregateNewRecord, combineStats)
    aggregate.collectAsMap()
  }

  def main(args: Array[String]): Unit = {
//    generateData()
    collectStats()
    collectStats2()
  }
}

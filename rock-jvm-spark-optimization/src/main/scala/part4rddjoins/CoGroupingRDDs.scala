package part4rddjoins

import org.apache.spark.{HashPartitioner, SparkContext}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession

object CoGroupingRDDs {
  val spark: SparkSession = SparkSession.builder()
    .master("local[*]")
    .appName("Co Grouping RDDs")
    //    .config("spark.sql.autoBroadcastJoinThreshold", -1)
    .getOrCreate()

  spark.sparkContext.setLogLevel("ERROR")
  val sc: SparkContext = spark.sparkContext
  val rootFolder = "src/main/resources/generated/examData"

  /*
  * Goal:
  * Take all the student attempts
  * - if a student passed (at least one attempt 9.0), sen them an email "PASSED"
  * - else send them an email with "FAILED"
  * */

  def readIds(): RDD[(Long, String)] = sc.textFile(s"$rootFolder/examIds.txt")
    .map { line =>
      val tokens = line.split(" ")
      (tokens(0).toLong, tokens(1))
    }

  def readExamScores(): RDD[(Long, Double)] = sc.textFile(s"$rootFolder/examScores.txt")
    .map { line =>
      val tokens = line.split(" ")
      (tokens(0).toLong, tokens(1).toDouble)
    }

  def readExamEmails(): RDD[(Long, String)] = sc.textFile(s"$rootFolder/examEmails.txt")
    .map { line =>
      val tokens = line.split(" ")
      (tokens(0).toLong, tokens(1))
    }

  def plainJoin() = {
    val scores = readExamScores().reduceByKey(Math.max)
    val candidates = readIds()
    val emails = readExamEmails()

    val results = candidates
      .join(scores) // RDD[(Long, (String, Double)]
      .join(emails) // RDD[(Long, ((String, Double), String))]
      .mapValues {
        case ((name, maxAttempt), email) =>
          if (maxAttempt >= 9.0) (email, "PASSED")
          else (email, "FAILED")
      }

    results.count()
  }

  def coGroupedJoin() = {
    val scores = readExamScores().reduceByKey(Math.max)
    val candidates = readIds()
    val emails = readExamEmails()

    val result: RDD[(Long, Option[(String, String)])] =
      candidates.cogroup(scores, emails) // co-partition the three RDDs RDD[(Long, (Iterable[String], Iterable[Double], Iterable[String]))]
        .mapValues {
          case (nameIterable, maxAttemptIterable, emailIterable) =>
            val name = nameIterable.headOption
            val maxScore = maxAttemptIterable.headOption
            val email = emailIterable.headOption

            for {
              e <- email
              s <- maxScore
            } yield (e, if (s >= 9.0) "PASSED" else "FAILED")
        }

    result.count()
    result.count()
  }

  def main(args: Array[String]): Unit = {
    plainJoin()
    coGroupedJoin()
    Thread.sleep(1000000)
  }
}

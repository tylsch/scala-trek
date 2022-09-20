package part4rddjoins

import generator.DataGenerator
import org.apache.spark.{HashPartitioner, SparkContext}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession

object SimpleRDDJoins {
  val spark: SparkSession = SparkSession.builder()
    .master("local[*]")
    .appName("RDD Joins")
//    .config("spark.sql.autoBroadcastJoinThreshold", -1)
    .getOrCreate()

  spark.sparkContext.setLogLevel("ERROR")
  val sc: SparkContext = spark.sparkContext
  val rootFolder = "src/main/resources/generated/examData"

  DataGenerator.generateExamData(rootFolder, 1000000, 5)

  def readIds(): RDD[(Long, String)] = sc.textFile(s"$rootFolder/examIds.txt")
    .map { line =>
      val tokens = line.split(" ")
      (tokens(0).toLong, tokens(1))
    }
    .partitionBy(new HashPartitioner(10))

  def readExamScores(): RDD[(Long, Double)] = sc.textFile(s"$rootFolder/examScores.txt")
    .map { line =>
      val tokens = line.split(" ")
      (tokens(0).toLong, tokens(1).toDouble)
    }
    .partitionBy(new HashPartitioner(10))

  // goal: the number of students who passed the exam (= at least one attempt > 9.0
  def plainJoin(): Unit = {
    val candidates = readIds()
    val scores = readExamScores()

    // simple join
    val joined = scores.join(candidates)
    val finalScores = joined
      .reduceByKey((p1, p2) => if (p1._1 > p2._1) p1 else p2)
      .filter(_._2._1 > 9.0)

    finalScores.count()
  }

  def preAggregate() = {
    val candidates = readIds()
    val scores = readExamScores()

    // do aggregation before join !!!
    val maxScores = scores.reduceByKey(Math.max)
    val finalScores = maxScores.join(candidates)
      .filter(_._2._1 > 9.0)

    finalScores.count()
  }

  def preFilter() = {
    val candidates = readIds()
    val scores = readExamScores()

    // do filtering first before join !!!
    val maxScores = scores.reduceByKey(Math.max).filter(_._2 > 9.0)
    val finalScores = maxScores.join(candidates)

    finalScores.count()
  }

  def coPartitioning() = {
    val candidates = readIds()
    val scores = readExamScores()

    val partitionerForScores = candidates.partitioner match {
      case None => new HashPartitioner(10)
      case Some(partitioner) => partitioner
    }

    val repartitionedScores = scores.partitionBy(partitionerForScores)

    val joined = repartitionedScores.join(candidates)
    val finalScores = joined
      .reduceByKey((p1, p2) => if (p1._1 > p2._1) p1 else p2)
      .filter(_._2._1 > 9.0)

    finalScores.count()
  }

  def combined() = {
    val candidates = readIds()
    val scores = readExamScores()

    val partitionerForScores = candidates.partitioner match {
      case None => new HashPartitioner(10)
      case Some(partitioner) => partitioner
    }
    val repartitionedScores = scores.partitionBy(partitionerForScores)

    val maxScores = repartitionedScores.reduceByKey(Math.max).filter(_._2 > 9.0)
    val finalScores = maxScores.join(candidates)

    finalScores.count()
  }

  def main(args: Array[String]): Unit = {
    plainJoin()
    preAggregate()
    preFilter()
    coPartitioning()
    combined()
    Thread.sleep(10000000)
  }
}

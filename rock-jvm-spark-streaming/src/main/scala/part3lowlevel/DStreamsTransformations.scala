package part3lowlevel

import common.Person
import org.apache.spark.sql.{Encoder, Encoders, SparkSession}
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.{Seconds, StreamingContext}

import java.io.File
import java.sql.Date
import java.time.{LocalDate, Period}

object DStreamsTransformations {


  val spark: SparkSession = SparkSession.builder()
    .appName("DStreams Transformations")
    .master("local[2]")
    .getOrCreate()

  val ssc = new StreamingContext(sparkContext = spark.sparkContext, batchDuration = Seconds(1))

  def readPeople(): DStream[Person] = {
    ssc.socketTextStream("localhost", 9999).map { line =>
      val tokens = line.split(":")

      Person(
        tokens(0).toInt,
        tokens(1),
        tokens(2),
        tokens(3),
        tokens(4),
        Date.valueOf(tokens(5)),
        tokens(6),
        tokens(7).toInt
      )
    }
  }

  def peopleAges(): DStream[(String, Int)] = {
    readPeople().map { person =>
      val age = Period.between(person.birthDate.toLocalDate, LocalDate.now()).getYears
      (s"${person.firstName} ${person.lastName}", age)
    }
  }

  def peopleSmallNames(): DStream[String] = readPeople().flatMap { person =>
    List(person.firstName, person.middleName)
  }

  def highIncomePeople(): DStream[Person] = readPeople().filter(_.salary >= 80000)

  // count
  def countPeople(): DStream[Long] = readPeople().count()

  // count by value, PER BATCH
  def countNames(): DStream[(String, Long)] = readPeople().map(_.firstName).countByValue()

  // reduce by key, works on DStream of tuples
  def countNamesReduce(): DStream[(String, Int)] =
    readPeople()
      .map(_.firstName)
      .map(name => (name, 1))
      .reduceByKey(_ + _)

  // foreach
  import spark.implicits._
  val personEncoder: Encoder[Person] = Encoders.product[Person]
  def saveToJson() = readPeople().foreachRDD { rdd =>
    val ds = spark.createDataset(rdd).as[Person](personEncoder)
    val f = new File("src/main/resources/data/people")
    val nFiles = f.listFiles().length
    val path = s"src/main/resources/data/people/people$nFiles.json"

    ds.write.json(path)
  }

  def main(args: Array[String]): Unit = {
//    val stream = countNamesReduce()
//    stream.print()
    saveToJson()
    ssc.start()
    ssc.awaitTermination()
  }
}

package part5boost

import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.SparkSession
import org.apache.spark.storage.StorageLevel

import scala.collection.immutable

object KryoSerializer {

  // 1 - define a SparkConf object with Kryo Serializer
  val sparkConf: SparkConf = new SparkConf()
    .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    .set("spark.kryo.registrationRequired", "true")
    .registerKryoClasses(Array(
      classOf[Person],
      classOf[Array[Person]]
    ))

  val spark: SparkSession = SparkSession.builder()
    .appName("Kryo Serialization")
    .master("local[*]")
    .config(sparkConf)
    .getOrCreate()

  spark.sparkContext.setLogLevel("ERROR")
  val sc: SparkContext = spark.sparkContext

  case class Person(name: String, age: Int)
  def generatorPeople(nPersons: Int): immutable.Seq[Person] = (1 to nPersons).map(i => Person(s"Person$i", i % 100))

  val people: RDD[Person] = sc.parallelize(generatorPeople(10000000))

  def testCaching() = {
    people.persist(StorageLevel.MEMORY_ONLY_SER).count()
    /*
    * Java Serialization
    * - memory usage 254MB
    * - time 12s
    *
    * Kryo Serialization
    * - memory usage 164.5MB
    * - time 12s
    * */
  }

  def testShuffling() = {
    people.map(p => (p.age, p)).groupByKey().mapValues(_.size).count()
    /*
      * Java Serialization
      * - shuffle 70.2MB
      * - time 14s
      *
      * Kryo Serialization
      * - shuffle 42.7MB
      * - time 13s
      * */
  }

  def main(args: Array[String]): Unit = {
    testShuffling()
    Thread.sleep(10000000)
  }
}

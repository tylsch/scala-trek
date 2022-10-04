package part5boost

import org.apache.spark.sql.SparkSession

object SerializationProblems {
  val spark = SparkSession.builder()
    .appName("Serialization Problems")
    .master("local[*]")
    .getOrCreate()

  spark.sparkContext.setLogLevel("ERROR")
  val sc = spark.sparkContext

  val rdd = sc.parallelize(1 to 100)

  class RddMultipler {
    def multiplyRdd() = rdd.map(_ * 2).collect().toList
  }

  val rddMultiplier = new RddMultipler
  rddMultiplier.multiplyRdd()
  // works

  // make class Serializable
  class MoreGeneralRddMultiplier extends Serializable {
    val factor = 2
    def multiplyRdd() = rdd.map(_ * factor).collect().toList
  }

  val moreGeneralRddMultiplier = new MoreGeneralRddMultiplier
//  moreGeneralRddMultiplier.multiplyRdd()

  class MoreGeneralRddMultiplierEnclosure {
    val factor = 2
    def multiplyRdd() = {
      val enclosedFactor = factor
      rdd.map(_ * enclosedFactor).collect().toList
    }
  }

  val moreGeneralRddMultiplier2 = new MoreGeneralRddMultiplierEnclosure
//  moreGeneralRddMultiplier2.multiplyRdd()

  /*
  * Exercise
  * */
  class MoreGeneralRddMultiplierNestClass {
    val factor = 2

    object NestedMultiplier extends Serializable {
      val extraTerm = 10
      val localFactor = factor
      def multiplyRdd() = {
        rdd.map(_ * localFactor + extraTerm).collect().toList
      }
    }
  }
  val nestedMultiplier = new MoreGeneralRddMultiplierNestClass
//  nestedMultiplier.NestedMultiplier.multiplyRdd()

  /*
  * Exercise 2
  * */
  case class Person(name: String, age: Int)
  val people = sc.parallelize(List(
    Person("Alice", 43),
    Person("Bob", 12),
    Person("Charlie", 23),
    Person("Diana", 67)
  ))

  class LegalDrinkingAgeChecker(legalAge: Int) {
    def processPeople(): List[Boolean] = {
      val ageThreshold = legalAge
      people.map(_.age >= ageThreshold).collect().toList
    }
  }
  val peopleChecker = new LegalDrinkingAgeChecker(21)
  peopleChecker.processPeople()


  def main(args: Array[String]): Unit = {

  }
}

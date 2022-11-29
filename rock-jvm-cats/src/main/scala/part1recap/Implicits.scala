package part1recap

import scala.language.postfixOps

object Implicits {

  // implicit classes
  case class Person(name: String) {
    def greet: String = s"Hi, my name is $name"
  }

  implicit class ImpersonableString(name: String) {
    def greet: String = Person(name).greet
  }

  val greeting = "Peter".greet // new ImpersonableString("Peter").greet

  // importing implicit conversions in scope
  import scala.concurrent.duration._
  val oneSecond = 1 second

  // implicit arguments and values
  def increment(x: Int)(implicit amount: Int) = x + amount
  implicit val defaultAmount = 10
  val incremented = increment(2) // implicit argument 10 is passed by the compiler

  def multiply(x: Int)(implicit times: Int) = x * times
  val times = multiply(2)

  // more complex example
  trait JsonSerializer[T] {
    def toJson(value: T): String
  }

  def listToJson[T](list: List[T])(implicit serializer: JsonSerializer[T]): String =
    list.map(v => serializer.toJson(v)).mkString("[", ",", "]")

  implicit val personSerializer: JsonSerializer[Person] = new JsonSerializer[Person] {
    override def toJson(value: Person): String =
      s"""
         |{"name" : "${value.name}"}
         |""".stripMargin
  }
  val personsJson = listToJson(List(Person("Alice"), Person("Bob")))
  // implicit argument is used to PROVE THE EXISTENCE of a type

  // implicit methods
  implicit def oneArgCaseClassSerializer[T <: Product]: JsonSerializer[T] = new JsonSerializer[T] {
    override def toJson(value: T): String =
      s"""
         |{"${value.productElementName(0)}" : "${value.productElement(0)}"}
         |""".stripMargin.trim
  }

  case class Cat(catName: String)
  val catsToJson = listToJson(List(Cat("Tom"), Cat("Garfield")))
  // implicit methods are used to PROVE THE EXISTENCE of a type
  // can be used for implicit conversions (DISCOURAGED)

  def main(args: Array[String]): Unit = {
    println(oneArgCaseClassSerializer[Cat].toJson(Cat("Garfield")))
    println(oneArgCaseClassSerializer[Person].toJson(Person("David")))
  }
}

package lectures.part4implicits

import scala.language.implicitConversions

object ImplicitsIntro extends App {
  val pair = "Tyler" -> "555"
  val intPair = 1 -> 2

  case class Person(name: String) {
    def greet = s"Hi, my name is $name"
  }

  implicit def fromStringToPerson(string: String): Person = Person(string)
  println("Peter".greet) // println(fromStringToPerson("Peter").greet()

//  class A {
//    def greet: Int = 2
//  }
//  implicit def fromStringToA(string: String): A = new A

  //implicit parameters
  def increment(x: Int)(implicit amount: Int) = x + amount
  implicit val defaultAmount: Int = 10
  increment(2)
  // NOT the same as default args
}

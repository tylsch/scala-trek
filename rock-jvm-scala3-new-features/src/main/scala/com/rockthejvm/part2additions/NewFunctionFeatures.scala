package com.rockthejvm.part2additions

import scala.concurrent.{ExecutionContext, Future}

object NewFunctionFeatures {

  /*
  * Generics in functions
  * */
  // Scala 2 only had generic METHODS
  def processOption[A](option: Option[A]): String = option match {
    case Some(value) => s"[$value]"
    case None => "[]"
  }

  // Scala 3: we CAN add generics to function values
  val processOptionFunc: [A] => Option[A] => String = // <- syntax for func signature
    [A] => (option: Option[A]) => option match // <- syntax for func implementation
      case Some(value) => s"[$value]"
      case None => "[]"

  /*
  * Context Functions: functions with using clauses/"implicit" args
  * */
  // Scala 2 = ONLY METHODS can have context args (implicit keyword)
  def methodWithoutContextArg(nonContextArg: Int)(nonContextArg2: String): String = ???
  def methodWithContextArg(nonContextArg: Int)(using contextArg: String): String = ???

  // Scala 3, also available for function values
  // eta-expansion
  val functionWithoutContextArg: Int => String => String = methodWithoutContextArg
  // eta-expansion works for methods with context args
  val functionWithContextArgs: Int => String ?=> String = methodWithContextArg
  //                                  ^^ this argument is a given

  // require given instances at the call site instead of definition
//  given ec: ExecutionContext = ???
//  val incrementAsync: Int => Future[Int] = x => Future(x * 1000) // can only work if I provide the given EC HERE

  val incrementAsync: ExecutionContext ?=> Int => Future[Int] = x => Future(x * 1000)
  // later, in some other part
  //given ec: ExecutionContext = ???
  //List(1,2,3).map(incrementAsync) // I will require EC at call site

  /*
  * Parameter untupling
  * */
  val tuples = List((1,2), (2,3), (3,4))
  //tuples.map((a, b) => a + b) // was not possible in Scala 2
  tuples.map {
    case (a, b) => a + b
  }

  // Scala 3 does automatic untupling
  tuples.map((a, b) => a + b)


  def main(args: Array[String]): Unit = {
    println(processOptionFunc(Some(1))) // ok
    println(processOptionFunc(Some("Some Scala"))) // ok
  }
}

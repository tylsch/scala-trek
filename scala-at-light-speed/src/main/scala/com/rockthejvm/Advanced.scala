package com.rockthejvm

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

object Advanced extends App {
  //lazy evaluation
  lazy val aLazyValue = 2
  lazy val lazyValueWithSideEffect = {
    println("I am so very lazy!")
    43
  }

  val eagerValue = lazyValueWithSideEffect + 1
  // useful in infinite collections

  // "pseudo-collections": Option, Try
  def unsafeMethod(): String = "hello Scala"
  if (unsafeMethod() == null) {
    // defensive code against null
  }
  val anOption = Option(unsafeMethod())
  // option = "collection" which contains at most one element: Some(value) or None

  val stringProcessing = anOption match {
    case Some(string) => s"I have obtained a valid string: $string"
    case None => "I obtained nothing"
  }
  // map, flatMap, filter

  def unsafeMethodThrowsException(): String = throw new RuntimeException()
  /*try {
    unsafeMethodThrowsException()
  } catch {
    case e: Exception => "defend against evil exception"
  }*/
  val aTry = Try(unsafeMethodThrowsException())
  // try = "collection" with eiter a value if the code went well, or an exception if the code threw one
  val anotherStringProcessing = aTry match {
    case Success(value) => s"I have obtained a valid string $value"
    case Failure(exception) => s"I have obtained an exception: $exception"
  }

  /*
  * Evaluate something on another thread
  * (async) programming
  * */
  val aFuture = Future {
    println("Loading...")
    Thread.sleep(1000)
    println("I have computed a value")
    67
  }

  // future is a "collection" which contains a value when it's evaluated
  // future is composable with map, flatMap, and filter
  // monads

  /*
  * Implicits basics
  * 1) Implicit Arguments
  * 2) Implicit Conversions
  * */

  def aMethodWithImplictArgs(implicit arg: Int) = arg + 1
  implicit val myImplicitInt: Int = 46
  println(aMethodWithImplictArgs)

  implicit class MyRichInteger(n: Int) {
    def isEven(): Boolean = n % 2 == 0
  }

  println(23.isEven())
  // use with care !!!
}

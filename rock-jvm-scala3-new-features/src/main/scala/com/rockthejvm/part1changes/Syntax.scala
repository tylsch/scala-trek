package com.rockthejvm.part1changes

object Syntax {

  // ifs
  val ifExpression = if (2 > 3) "bigger" else "smaller"
  val ifExpression_v2 = if 2 > 3 then "bigger" else "smaller"

  // multiline ifs
  val ifExpression_v3 =
    if (2 > 3) {
      val result = "bigger"
      result
    } else {
      val result = "smaller"
      result
    }

  // Scala 3: brace-less
  val ifExpression_v4 =
    if 2 > 3 then
      val result = "bigger"
      result
    else
      val result = "smaller"
      result
  // indentation matters!!!

  // while
  val whileExpression: Unit = while (2 > 3) {
    println("bigger")
    println("much bigger")
  }

  // Scala 3: brace-less
  val whileExression_v2: Unit = while 2 > 3 do
    println("bigger")
    println("Much bigger")
  // indentation matters!!!

  // for
  val forComprehension =
    for {
      num <- List(1,2,3)
      char <- List('a','b','c')
    } yield s"$num-$char"

  // Scala 3
  val forComprehension_v2 =
    for
      num <- List(1, 2, 3)
      char <- List('a', 'b', 'c')
    yield s"$num-$char"

  // match
  val meaningOfLife = 42
  val aPatternMatch = meaningOfLife match {
    case 1 => "the one"
    case 2 => "the two"
    case _ => "the other"
  }

  // Scala 3
  val aPatternMatch_v2 = meaningOfLife match
    case 1 => "the one"
    case 2 => "the two"
    case _ => "the other"

  // try-catch
  val tryCatch =
    try {
      "".charAt(2)
    } catch {
      case e: IndexOutOfBoundsException => '_'
      case e: Exception => 'z'
    }

  // Scala 3
  val tryCatch_v2 =
    try
      "".charAt(2)
    catch
      case e: IndexOutOfBoundsException => '_'
      case e: Exception => 'z'

  /*
  * Significant Indentation
  * */
  def computeMeaningOfLife(arg: Int): Int = // significant indentation activated HERE
    val partialResult = 49

    // code block

    partialResult + arg + 2

  def isPrime(n: Int): Boolean =
    def aux(potentialDivisior: Int): Boolean =
      if (potentialDivisior > n / 2) true
      else if (n % potentialDivisior == 0) false
      else aux(potentialDivisior + 1)

    aux(2)
  end isPrime
  // ^ if, while, match, for, classes, objects, ...

  // significant indentation region token `:`, for classes traits, objects, enums
  class Animal {
    def eat(): Unit = {
      println("I'm eating")
    }
  }

  // Scala 3
  class AnimalV2:
    def eat(): Unit =
      println("I'm eating")

    def grow(): Unit =
      println("I'm growing")

  end AnimalV2

  /*
  * Indentation = # of whitespace characters (spaces + tabs)
  3 spaces + 2 tabs > 2 spaces + 2 tabs
  3 spaces + 3 tabs > 3 spaces + 2 tabs
  3 spaces + 2 tabs ??? 2 spaces + 3 tabs

  use one kind throughout code
  * */


  def main(args: Array[String]): Unit = {

  }
}

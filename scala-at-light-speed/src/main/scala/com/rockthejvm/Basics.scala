package com.rockthejvm

object Basics extends App {
  val meaningOfLife: Int = 42 // const int meaningOfLife = 42

  // Int, Boolean, Char, Double, Float, String
  val aBoolean = false //type is optional

  // strings and string operations
  val aString = "I love Scala"
  val aComposedString = "I" + " " + "love" + " " + "Scala"
  val anInterpolatedString = s"The meaning of life is $meaningOfLife"

  // expressions = structures that can be reduced to a value
  val anExpression = 2 + 3

  // if-expression
  val ifExpression = if (meaningOfLife > 43) 56 else 999 // in other languages C# meaningOfLife > 43 ? 56 : 999
  val chainedIfExpression =
    if (meaningOfLife > 43) 56
    else if (meaningOfLife < 0) -2
    else if (meaningOfLife > 999) 78
    else 0

  val aCodeBlock = {
    val aLocalValue = 67
    aLocalValue + 3
  }

  def myFunction(x: Int, y: String): String = y + " " + x

  // recursive function
  def factorial(n: Int): Int =
    if (n <= 1) 1
    else n * factorial(n - 1)

  // Unit return types = no meaningful value === "void" in other languages
  // type of side effects
  println("I love Scala")

  def myUnitReturningFucntion(): Unit = {
    println("I don't love returning Unit")
  }
}

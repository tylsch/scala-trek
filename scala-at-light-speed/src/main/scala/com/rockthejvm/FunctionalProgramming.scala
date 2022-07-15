package com.rockthejvm

object FunctionalProgramming extends App {
  //Scala is OOP
  class Person(name: String) {
    def apply(age: Int) = println(s"I have aged $age years")
  }
  val bob = new Person("Bob")
  bob.apply(43)
  bob(43) // Invoking bob as a function === bob.apply(43)

  /*
  * Scala runs on the JVM
  * Functional Programming:
  * - compose functions
  * - pass functions as args
  * - return functions as results
  * Conclusion = FunctionX = Function1, Function2, ...Function22
  * */

  val simpleIncrementer = new Function1[Int, Int] {
    override def apply(v1: Int): Int = v1 + 1
  }

  simpleIncrementer.apply(23)
  simpleIncrementer(23) // same as apply method
  // defined a function!

  // All Scala functions are instances of the FunctionX types
  val stringConcatenator = new Function2[String, String, String] {
    override def apply(v1: String, v2: String): String = v1 + v2
  }

  stringConcatenator("I love", " Scala") //returns I love Scala

  //Syntax Sugars
  val doubler: Int => Int = (x: Int) => 2 * x // Short hand for new FunctionX with apply
  doubler(4)

  // Higher-Order Functions: take functions as args, return function, or both
  val aMappedList = List(1,2,3).map(_ + 1) // same as .map(x => x + 1) === HOF
  println(aMappedList)
  val aFlatMappedList = List(1,2,3).flatMap { x =>
    List(x, 2 * x)
  }
  println(aFlatMappedList)
  val aFilteredList = List(1,2,3,4,5).filter(_ <= 3) // Short hand for x => x <= 3
  println(aFilteredList)

  // all pairs between the numbers 1,2,3 and the letters 'a', 'b', 'c'
  val allPairs = List(1,2,3).flatMap(num => List('a', 'b', 'c').map(letter => s"$num-$letter"))
  println(allPairs)

  // for comprehensions
  val alternativePairs = for {
    number <- List(1,2,3)
    letter <- List('a','b','c')
  } yield s"$number-$letter"
  // same as allPairs chain function above

  // Lists
  val aList = List(1,2,3,4,5)
  val firstElement = aList.head
  val rest = aList.tail
  val aPrependedList = 0 :: aList //prepend
  val extendedList = 0 +: aList :+ 6

  //sequences
  val aSeq: Seq[Int] = Seq(1,2,3)
  val accessedElement = aSeq(1)

  // vectors = Fast Seq implement
  val aVector = Vector(1,2,3,4,5)

  // sets = no duplicates
  val aSet = Set(1,2,3,4,1,2,3,4) // Set(1,2,3,4)
  val setHasFive = aSet.contains(5) // false
  val anAddedSet = aSet + 5 // Set(1,2,3,4,5)
  val aRemovedSet = aSet - 3 // Set(1,2,4)

  // ranges
  val aRange = 1 to 1000
  val twoByTwo = aRange.map(_ * 2).toList

  // tuples = groups of values under the same value
  val aTuple = ("Bon Jovi", "Rock", 1982)

  // maps
  val aMap: Map[String, Int] = Map(
    ("Daniel", 324534),
    "Jane" -> 546784567 // Same as tuple format
  )
}

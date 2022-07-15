package com.rockthejvm

object PatternMatching extends App {
  // switch "expression"
  val anInteger = 55
  val order = anInteger match {
    case 1 => "first"
    case 2 => "second"
    case 3 => "third"
    case _ => anInteger + "th"
  } // PM is an expression
  println(order)

  case class Person(name: String, age: Int)
  val bob = Person("Bob", 43)

  val personGreeting = bob match {
    case Person(name, age) => s"Hi, my name is $name and I am $age years old"
    case _ => "Something else"
  }
  println(personGreeting)

  // deconstructing tuples
  val aTuple = ("Bon Jovi", "Rock")
  val bandDescription = aTuple match {
    case (band, genre) => s"$band belows to the genre $genre"
    case _ => "I don't know what you are talking about"
  }
  println(bandDescription)

  // decomposing list
  val aList = List(1,2,3)
  val listDescription = aList match {
    case List(_, 2, _) => "List contain 2 on its second position"
    case _ => "unknown list"
  } // if PM doesn't match anything, it will throw a MatchError, PM will try all cases in sequence
}

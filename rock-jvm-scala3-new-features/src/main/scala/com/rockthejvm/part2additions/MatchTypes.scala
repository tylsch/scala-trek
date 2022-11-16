package com.rockthejvm.part2additions

object MatchTypes {

  // Ints, strings, Lists
  // BigInt -> last digit, of type Int
  // String -> last character, of type Char
  // List -> last element

  def lastDigitOf(number: BigInt): Int = (number % 10).toInt
  def lastCharOf(string: String): Char =
    if string.isEmpty then throw new NoSuchElementException
    else string.charAt(string.length - 1)
  def lastElement[T](list: List[T]): T =
    if list.isEmpty then throw new NoSuchElementException
    else list.last

  // Scala 2? = can't do it
  // Scala 3? = yes

  type ConstituentPartOf[T] = T match
    case BigInt => Int
    case String => Char
    case List[t] => t

  val aDigit: ConstituentPartOf[BigInt] = 2
  val aChar: ConstituentPartOf[String] = 'a'
  val aElement: ConstituentPartOf[List[Int]] = 42

  def lastComponentOf[T](biggerValue: T): ConstituentPartOf[T] = biggerValue match
    case b: BigInt => (b % 10).toInt
    case s: String =>
      if s.isEmpty then throw new NoSuchElementException
      else s.charAt(s.length - 1)
    case l: List[_] =>
      if l.isEmpty then throw new NoSuchElementException
      else l.last

  val lastDigit: ConstituentPartOf[BigInt] = lastComponentOf(BigInt(2357845))
  val lastChar: ConstituentPartOf[String] = lastComponentOf("Scala")
  val lastElement: ConstituentPartOf[List[Int]] = lastComponentOf(List(1,2,3))

  // Why is this different from OOP
  // def returnLastConstituentOf(thing: Any): ConstituentPart = thing match ...

  // Why is this different from regular generics?
  // more subtle

  def lastElementOfList[A](list: List[A]): A = list.last
  lastElementOfList(List(1,2,3)) // 3, an Int
  lastElementOfList(List("a", "b"))

  // recursion
  type LowestLevelPartOf[T] = T match
    case List[t] => LowestLevelPartOf[t]
    case _ => T

  val lastElementOfNestedList: LowestLevelPartOf[List[List[List[Int]]]] = 2

  // not ok
//  type AnnoyingMatchType[T] = T match
//    case _ => AnnoyingMatchType[T]

//  type InfiniteRecursiveType[T] = T match
//    case Int => InfiniteRecursiveType[T]

//  def aNaiveMethod[T]: InfiniteRecursiveType[T] = ???
//  val illegal: Int =  aNaiveMethod[Int] // recursion limit exceed

//  def accumulate[T](accumulator: T, smallerValue: ConstituentPartOf[T]): T = accumulator match
//    case b: BigInt => b + smallerValue // "flow typing"


  def main(args: Array[String]): Unit = {

  }
}

package com.rockthejvm.part1changes

import scala.annotation.tailrec

object ExtensionMethods {

  /*
  * implicit vals/agruments -> given/using
  * implicit classes -> extension methods
  * */

  implicit class MyRichInteger(number: Int) {
    def isEven: Boolean = number % 2 == 0
  }
  val is2Even: Boolean = 2.isEven

  // Scala 3
  extension (number: Int)
    def isEven_v2: Boolean = number % 2 == 0

  val is2Even_v2: Boolean = 2.isEven_v2

  // recommended: use extension methods

  // generic extensions\
  extension [A](list: List[A])
    def ends: (A, A) = (list.head, list.last)

  val aList: List[Int] = List(1,2,3,4)
  val firstLast: (Int, Int) = aList.ends

  /*
  * Reason 1: make APIs very expressive
  * Reason 2: enhance CERTAIN types with new methods BUT NOT others
  * */
  // type class
  trait Semigroup[A]:
    def combine(x: A, y: A): A

  extension [A](list: List[A])
    def combineAll(using semigroup: Semigroup[A]): A =
      list.reduce(semigroup.combine)

  given intSemigroup: Semigroup[Int] with
    override def combine(x: Int, y: Int): Int = x + y

  val sum: Int = aList.combineAll
  val listOfStrings: List[String] = List("black", "white")
  //val combinationOfStrings = listOfStrings.combineAll // doesn't work

  // Grouping extensions
  object GroupedExtensions:
    extension[A] (list: List[A])
      def ends: (A, A) = (list.head, list.last)
      def combineAll(using semigroup: Semigroup[A]): A =
        list.reduce(semigroup.combine)
    end extension
  end GroupedExtensions

  implicit class PrimeChecker(n: Int) {
    def isPrime: Boolean = {
      @tailrec
      def isPrimeHelper(potDivisor: Int): Boolean =
        if (potDivisor > n / 2) true
        else if (n % potDivisor == 0) false
        else isPrimeHelper(potDivisor + 1)

      assert(n >= 0)
      if (n == 0 || n == 1) false
      else isPrimeHelper(2)
    }
  }

  sealed abstract class Tree[A]
  case class Leaf[A](value: A) extends Tree[A]
  case class Branch[A](left: Tree[A], right: Tree[A]) extends Tree[A]

  extension (n: Int)
    def isPrime: Boolean =
      @tailrec
      def isPrimeHelper(potDivisor: Int): Boolean =
        if (potDivisor > n / 2) true
        else if (n % potDivisor == 0) false
        else isPrimeHelper(potDivisor + 1)

      assert(n >= 0)
      if (n == 0 || n == 1) false
      else isPrimeHelper(2)
    end isPrime
  end extension

  extension [A](tree: Tree[A])
    def map[B](f: A => B): Tree[B] = tree match
      case Leaf(value) => Leaf(f(value))
      case Branch(left, right) => Branch(left.map(f), right.map(f))

    def forAll(predicate: A => Boolean): Boolean = tree match
      case Leaf(value) => predicate(value)
      case Branch(left, right) => left.forAll(predicate) && right.forAll(predicate)

  end extension

  extension (tree: Tree[Int])
    def sum: Int = tree match
      case Leaf(value) => value
      case Branch(left, right) => left.sum + right.sum



  def main(args: Array[String]): Unit = {
    val aTree: Tree[Int] = Branch(Branch(Leaf(1), Leaf(2)), Leaf(3))

    println(aTree.map(_ * 10))
    println(aTree.forAll(_ < 10))
    println(aTree.sum)
  }
}

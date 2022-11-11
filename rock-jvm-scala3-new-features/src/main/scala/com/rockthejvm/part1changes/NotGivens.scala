package com.rockthejvm.part1changes

object NotGivens {

  def processList[A, B](la: List[A], lb: List[B]): List[(A, B)] =
    for
      a <- la
      b <- lb
    yield (a, b)

  // desire: be able to call processLists ONLY IF teh types A and B are different
  def processSameType[A](la: List[A], lb: List[A]): List[(A, A)] = processList(la, lb)

  // desire: be able to call processLists ONLY IF teh types A and B are DIFFERENT
  val combinedLists: List[(Int, String)] = processList(List(1,2,3), List("black", "white")) //ok
  val combinedNumbers: List[(Int, Int)] = processList(List(1,2,3), List(4,5)) // not ok, we want this code to NOT COMPILE

  class <:>[A, B]
  // make the compiler create a <:>[A, A]
  given equalType[A]: <:>[A, A] = new <:>[A, A]

  object DifferentTypes:
    import scala.util.NotGiven
    // compiler cannot find a give T => compiler will generate a NotGiven[T]
    def processListsDifferentType[A, B](la: List[A], lb: List[B])(using NotGiven[A <:> B]): List[(A, B)] =
      processList(la, lb)


  def main(args: Array[String]): Unit = {
    import DifferentTypes._
    val combinedLists = processListsDifferentType(List(1,2,3), List("black"))
    //val combinedNumbers = processListsDifferentType(List(1,2,3), List(4,5))
  }
}

package lectures.part1as

import scala.util.Try

object DarkSugars extends App {
  // syntax sugar #1: methods with single parameter
  def singleArgMethod(arg: Int): String = s"$arg little ducks"

  val description = singleArgMethod {
    // write some code
    42
  }

  val aTryInstance = Try {
    // java's try {...}
    throw new RuntimeException
  }

  List(1,2,3).map { x =>
    x + 1
  }

  // syntax sugar #2: Single Abstract Method
  trait Action {
    def act(x: Int): Int
  }

  val anInstance: Action = new Action {
    override def act(x: Int): Int = x + 1
  }

  val aFunkyInstance: Action = (x: Int) => x + 1 // magic

  // example: Runnables
  val aThread = new Thread(new Runnable {
    override def run(): Unit = println("Hello, Scala")
  })

  val aSweeterThread = new Thread(() => println("Sweet, Scala"))

  abstract class AnAbstractType {
    def implemented: Int = 23
    def f(a: Int): Unit
  }

  val anAbstractInstance: AnAbstractType = (a: Int) => println("sweet")

  // syntax sugar #3: the :: and #:: methods are special

  val prependedList = 2 :: List(3,4)
  //2.::(List(3,4))
  // List(3,4).::(2) ???

  // Scala Spec: last character decides the associativity of the method
  //1 :: 2 :: 3 :: List(4,5)
  // List(4,5).::(3).::(2).::(1)

  class MyStream[T] {
    def -->:(value: T): MyStream[T] = this
  }

  val myStream = 1 -->: 2 -->: 3 -->: new MyStream[Int]

  // syntax sugar #4: multi-word method naming
  class TeenGirl(name: String) {
    def `and then said`(gossip: String): Unit = println(s"$name said $gossip")
  }

  val lilly = new TeenGirl("Lilly")
  lilly `and then said` "Scala is so sweet"

  // syntax sugar #5: infix types
  class Composite[A, B]
  val composite: Int Composite String = ???

  class -->[A, B]
  val towards: Int --> String = ???

  // syntax sugar #6: update() is very special, much like apply()
  val anArray = Array(1,2,3)
  anArray(2) = 7 // rewritten to anArray.update(2, 7)
  // used in mutable collections
  // remember apply and update

  // syntax sugar #7: Setters for mutable containers
  class Mutable {
    private var internalMember: Int = 0 // private for OO encapsulation
    def member: Int = internalMember // "getter"
    def member_=(value: Int): Unit = internalMember = value // "setter"
  }

  val aMutableContainer = new Mutable
  aMutableContainer.member = 42 // rewritten as aMutableContainer.member_=(42)
}

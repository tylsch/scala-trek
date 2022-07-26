package lectures.part2afp

object LazyEvaluation extends App {
  // lazy DELAYS  the evaluation of values
  lazy val x: Int = {
    println("Hello")
    42
  }
  println(x)
  println(x)

  // Examples
  // side effects
  def sideEffectCondition: Boolean = {
    println("Boo")
    true
  }
  def simpleCondition: Boolean = false

  lazy val lazyCondition = sideEffectCondition
  println(if (simpleCondition && lazyCondition) "yes" else "no")

  // in conjunction with call by name
  def byNameMethod(n: => Int): Int = {
    // Call by need
    lazy val t = n
    t + t + t + 1
  }
  def retrieveMagicValue = {
    println("waiting")
    Thread.sleep(1000)
    42
  }

  println(byNameMethod(retrieveMagicValue))
  // use lazy vals

  //filtering with lazy vals
  def lessThanThirty(i: Int): Boolean = {
    println(s"$i is less than thirty?")
    i < 30
  }

  def greaterThanTwenty(i: Int): Boolean = {
    println(s"$i is greater than twenty?")
    i > 20
  }

  val numbers = List(1,25,40,5,23)
  val lt30 = numbers.filter(lessThanThirty) // List(1,25,5,23)
  val gt20 = numbers.filter(greaterThanTwenty)
  println(gt20)

  val lt30Lazy = numbers.withFilter(lessThanThirty) // withFilter uses lazy vals under the hood
  val gt20Lazy = lt30Lazy.withFilter(greaterThanTwenty)
  println
  gt20Lazy.foreach(println)

  // for-comprehensions use withFilter with guards
  for {
    a <- List(1,2,3) if a % 2 == 0 // use lazy vals
  } yield  a + 1
  List(1,2,3).withFilter(_ % 2 == 0).map(_ + 1) // List[Int]

  /*
  * Exercise: Implement a lazily evaluated, single linked STREAM of elements
  * */
  abstract class MyStream[+A] {
    def isEmpty: Boolean
    def head: A
    def tail: MyStream[A]

    def #::[B >: A](element: B): MyStream[B] // prepend operator
    def ++[B >: A](anotherStream: MyStream[B]): MyStream[B]

    def foreach(f: A => Unit): Unit
    def map[B](f: A => B): MyStream[B]
    def flatMap[B](f: A => MyStream[B]): MyStream[B]
    def filter(predicate: A => Boolean): MyStream[A]

    def take(n: Int): MyStream[A]
    def takeAsList(n: Int): List[A]
  }

  object MyStream {
    def from[A](start: A)(generator: A => A): MyStream[A] = ???
  }
}


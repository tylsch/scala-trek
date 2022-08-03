package lectures.part4implicits

object PimpMyLibrary extends App {
  // 2.isPrime

  implicit class RichInt(val value: Int) extends AnyVal {
    def isEven: Boolean = value % 2 == 0
    def sqrt: Double = Math.sqrt(value)
    def times(function: () => Unit): Unit = {
      def timesAux(n: Int): Unit =
        if (n <= 0) ()
        else {
          function()
          timesAux(n - 1)
        }

      timesAux(value)
    }
    def *[T](list: List[T]): List[T] = {
      def concat(n: Int): List[T] =
        if (n <= 0) List()
        else concat(n - 1) ++ list

      concat(value)
    }
  }

  implicit class RicherInt(richInt: RichInt) {
    def isOdd: Boolean = richInt.value % 2 == 0
  }

  new RichInt(42).sqrt

  42.isEven // Type Enrichment, new RichInt(42).isEven

  import scala.concurrent.duration._
  3.seconds

  // compiler doesn't do multiple implicit searches
//  42.isOdd

  /*
  * Enrich the String Class
  * - asInt
  * - encrypt
  * Keep enriching the Int class
  * - times(function)
  * - * => 3 * List(1,2) == List(1,2,1,2,1,2)
  * */

  implicit class RichString(value: String) {
    def asInt: Int = Integer.valueOf(value) // java.lang.Integer -> Int
    def encrypt(cypherDistance: Int): String = value.map(c => (c + cypherDistance).asInstanceOf[Char])
  }
  println("3".asInt + 4)
  println("John".encrypt(2))

  3.times(() => println("Scala Rocks!"))
  println(4 * List(1,2))

  // "3" / 4
  implicit def stringToInt(string: String): Int = Integer.valueOf(string)
  println("6" / 2) // stringToInt("6") / 2

  // ==: implicit class RichAltInt(value: Int)
  class RichAlternativeInt(value: Int)
  implicit def enrich(value: Int): RichAlternativeInt = new RichAlternativeInt(value)

  // Danger Zone
  implicit def intToBoolean(i: Int): Boolean = i == 1

  val aConditionedValue = if (3) "OK" else "something wrong"
  println(aConditionedValue)
}

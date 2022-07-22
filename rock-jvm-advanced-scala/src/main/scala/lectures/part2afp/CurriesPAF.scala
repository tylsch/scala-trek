package lectures.part2afp

object CurriesPAF extends App {
  // curried functions
  val superAdder: Int => Int => Int =
    x => y => x + y

  val add3 = superAdder(3) // Int => Int = y => 3 + y
  println(add3(5))
  println(superAdder(3)(5)) // Curried Function

  def curriedAdder(x: Int)(y: Int): Int = x + y // Curried Method

  val add4: Int => Int = curriedAdder(4)
  // lifting = ETA-Expansion
  // functions != methods (JVM Limitation)
  def inc(x: Int) = x + 1
  List(1,2,3).map(inc) // ETA-Expansion map(x => inc(x))

  //Partial Function Applications
  val add5 = curriedAdder(5) _ // ETA-Expansion Int => Int

  // Exercise
  val simpleAddFunction = (x: Int, y: Int) => x + y
  def simpleAddMethod(x: Int, y: Int) = x + y
  def curriedAddMethod(x: Int)(y: Int) = x + y

  val add7 = (x: Int) => simpleAddFunction(7, x)
  val add7_2 = simpleAddFunction.curried(7)
  val add7_6 = simpleAddFunction(7, _: Int)
  val add7_3 = curriedAddMethod(7) _ // PAF
  val add7_4 = curriedAddMethod(7)(_) // PAF = alt syntax
  val add7_5 = simpleAddMethod(7, _: Int) // alt syntax for turning methods into functional values

  // underscores are powerful
  def concatentor(a: String, b: String, c: String) = a + b + c
  val insertName = concatentor("Hello, I'm ", _, ", how are you?")
  println(insertName("Tyler"))

  val fillInTheBlanks = concatentor("Hello, ", _: String, _: String) // ETA-Expanded = (x, y) => concatentor
  println(fillInTheBlanks("Tyler", " Scala is awesome!"))

  // Exercises
  /*
  * 1) Process a list of numbers and return their string representations with different formats
  *    Use teh %4.2f, %8.6f, an d%14.12f with a curried formatter function function
  * */
  println("%4.2f".format(Math.PI))
  def curryFormatter(s: String)(n: Double): String = s.format(n)
  val numbers = List(Math.PI, Math.E, 1, 9.8, 1.3e-12)
  val simpleFormat = curryFormatter("%4.2f") _
  val seriousFormat = curryFormatter("%8.6f") _
  val preciseFormat = curryFormatter("%14.12f") _

  println(numbers.map(simpleFormat))
  println(numbers.map(seriousFormat))
  println(numbers.map(preciseFormat))

  /*
  * 2) Difference between
  *    - functions vs. methods
  *    - parameters: by-name vs 0-lambda
  * */
  def byName(n: => Int) = n + 1
  def byFunction(f: () => Int) = f() + 1

  def method: Int = 42
  def parenMethod(): Int = 42

  /*
  * Calling byName and byFunction
  * - Int
  * - Method
  * - parenMethod
  * - lambda
  * - PAF
  * */

  byName(23) // ok
  byName(method) // ok
  byName(parenMethod()) // ok
  //byName(parenMethod) // ok but beware ==> byName(parenMethod())
  // byName(() => 42) // Not Ok
  byName((() => 42)()) // ok
  //byName(parenMethod _) // Not ok

  //byFunction(43) // Not ok
  //byFunction(method) // not ok
  //byFunction(parenMethod()) // not ok
  byFunction(parenMethod) // ok
  byFunction(parenMethod _) // ok

}

package part1recap

object CatsIntro {
  // Eq
  val aComparsion = 2 == "a String" // will trigger a compile warning

  // part 1 - type class import
  import cats.Eq
  // part 2 - import type class instances for the types you need
  import cats.instances.int._
  // part 3 - use the type class API
  val intEquality = Eq[Int]
  val aTypeSafeComparsion = intEquality.eqv(2, 3) // false
  //val aTypeUnsafeComparison = intEquality.eqv(2, "df") // does not compile

  // part 4 - use extension methods (if applicable)
  import cats.syntax.eq._
  val anotherTypeSafeComparsion = 2 === 3 // false
  val neqComparsion = 2 =!= 3 // true
  //val invalidComparison = 2 === "a String" // does not compile
  // extension methods are only visible in the presence of the right type class instance

  // part 5 - extending the type class operations to composite types, e.g. lists
  import cats.instances.list._
  val aListComparsion = List(2) === List(3) // returns false

  // part 6 - create a type class instance for a custom type
  case class ToyCar(model: String, price: Double)
  implicit val toyCarEq: Eq[ToyCar] = Eq.instance[ToyCar] { (car1, car2) =>
    car1.price == car2.price
  }

  val compareTwoToyCars = ToyCar("Ferrari", 29.99) === ToyCar("Lamborghini", 29.99)


}

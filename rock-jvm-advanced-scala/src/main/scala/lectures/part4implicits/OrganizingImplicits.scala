package lectures.part4implicits

object OrganizingImplicits extends App {
  implicit val reverseOrdering: Ordering[Int] = Ordering.fromLessThan(_ > _)
//  implicit val normalOrdering: Ordering[Int] = Ordering.fromLessThan(_ > _)
  println(List(1,4,5,3,2).sorted)
  // scala.Predef0

  /*
  * Implicits (used as implicit parameters):
  * - val/var
  * - object
  * - accessor methods = defs with no parameters
  * */

  /*
  * Exercise
  * */
  case class Person(name: String, age: Int)
//  object Person {
//    implicit val nameOrdering: Ordering[Person] = Ordering.fromLessThan((a, b) => a.name.compareTo(b.name) < 0)
//  }

//  implicit val ageOrdering: Ordering[Person] = Ordering.fromLessThan((a, b) => a.age < b.age)
  val persons = List(
    Person("Steve", 30),
    Person("Amy", 22),
    Person("John", 66)
  )

//  println(persons.sorted)

  /*
  * Implicit Scope
  * - Normal Scope = LOCAL SCOPE
  * - Imported Scope
  * - Companions of all types involved in the method signature
  *   - List
  *   - Ordering
  *   - All the types involved = A or any supertype
  * */

  object AlphabeticNameOrdering {
    implicit val nameOrdering: Ordering[Person] = Ordering.fromLessThan((a, b) => a.name.compareTo(b.name) < 0)
  }
  object AgeOrdering {
    implicit val ageOrdering: Ordering[Person] = Ordering.fromLessThan((a, b) => a.age < b.age)
  }

  import AgeOrdering._
  println(persons.sorted)

  /*
  * Exercise
  * - totalPrice = most used (50%)
  * - byUnitCount = 25%
  * - byUnitPrice = 25%
  * */
  case class Purchase(nUnits: Int, unitPrice: Double)
  object Purchase {
    implicit val totalPriceOrdering: Ordering[Purchase] = Ordering.fromLessThan((a, b) => a.nUnits * a.unitPrice < b.nUnits * b.unitPrice)
  }

  object UnitCountOrdering {
    implicit val unitCountOrdering: Ordering[Purchase] = Ordering.fromLessThan((a, b) => a.nUnits < b.nUnits)
  }

  object UnitPriceOrdering {
    implicit val unitPriceOrdering: Ordering[Purchase] = Ordering.fromLessThan((a, b) => a.unitPrice < b.unitPrice)
  }
}

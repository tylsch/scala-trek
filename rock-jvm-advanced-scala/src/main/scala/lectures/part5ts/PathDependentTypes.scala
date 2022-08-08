package lectures.part5ts

object PathDependentTypes extends App {
  class Outer {
    class Inner
    object InnerObject
    type InnerType

    def print(i: Inner) = println(i)
    def printGeneral(i: Outer#Inner) = println(i)
  }

  def aMethod: Int = {
    class HelperClass
    type HelperType = String
    2
  }

  // per-instance
  val o = new Outer
  val inner = new o.Inner // Outer Instance is a TYPE

  val oo = new Outer
//  val otherInner: oo.Inner = new o.Inner

  o.print(inner)
//  oo.print(inner)

  // path dependent type

  // Outer#Inner
  o.printGeneral(inner)
  oo.printGeneral(inner)

  /*
  * Exercise
  * - DB Keyed by Int or String, but maybe others
  * */
  trait ItemLike {
    type Key
  }
  trait Item[K] extends ItemLike {
    type Key = K
  }
  trait IntItem extends Item[Int]
  trait StringItem extends Item[String]

  def fetch[ItemType <: ItemLike](key: ItemType#Key): ItemType

  fetch[IntItem](42) //ok
  fetch[StringItem]("home") //ok
//  fetch[IntItem]("Scala") // not ok, type mismatch
}

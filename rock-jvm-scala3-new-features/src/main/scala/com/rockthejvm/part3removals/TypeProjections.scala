package com.rockthejvm.part3removals

object TypeProjections:
  class Outer:
    class Inner

  val o1 = new Outer
  val o2 = new Outer
  val i1: Outer#Inner = new o1.Inner
  val i2: Outer#Inner = new o2.Inner
  //      ^^^^^^^^^^^ type projection

  // Scala 2 advance exercise to promote abstract type projects
//  trait ItemLike {
//    type Key
//  }
//
//  trait Item[K] extends ItemLike {
//    override type Key = K
//  }
//
//  class StringItem extends Item[String]
//
//  class IntItem extends Item[Int]
//
//  def get[ItemType <: ItemLike](key: ItemType#Key): ItemType = ???
//  //                                 ^^^^^^^^ DOES NOT COMPILE IN SCALA 3
//
//  get[IntItem](42) // an IntItem with 42
//  get[StringItem]("Scala") // a StringItem with "Scala"
//  //get[StringItem](42) // should not compile


end TypeProjections


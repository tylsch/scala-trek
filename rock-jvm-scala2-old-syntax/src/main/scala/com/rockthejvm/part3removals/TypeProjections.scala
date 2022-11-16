package com.rockthejvm.part3removals

object TypeProjections {
  trait ItemLike {
    type Key
  }

  trait Item[K] extends ItemLike {
    override type Key = K
  }

  class StringItem extends Item[String]
  class IntItem extends Item[Int]

  def get[ItemType <: ItemLike](key: ItemType#Key): ItemType = ???

  get[IntItem](42) // an IntItem with 42
  get[StringItem]("Scala") // a StringItem with "Scala"
  //get[StringItem](42) // should not compile

  trait ItemAll extends ItemLike {
    type Key >: Any
  }

  trait ItemNothing extends ItemLike {
    type Key <: Nothing
  }

  def funcAll[ItemType <: ItemAll]: Any => ItemType#Key = a => a
  def funcNothing[ItemType <: ItemNothing]: ItemType#Key => Nothing = a => a
  def funcWeird[ItemType <: ItemAll with ItemNothing]: Any => Nothing =
    funcAll[ItemType].andThen(funcNothing[ItemType])

  def main(args: Array[String]): Unit = {
    val anInt: Int = funcWeird("Scala")
    println(anInt + 1)
  }
}

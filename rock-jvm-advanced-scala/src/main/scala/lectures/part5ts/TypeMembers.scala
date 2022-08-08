package lectures.part5ts

object TypeMembers extends App {
  class Animal
  class Dog extends Animal
  class Cat extends Animal

  class AnimalCollection {
    type AnimalType // abstract type member
    type BoundedAnimal <: Animal
    type SuperBoundedAnimal >: Dog <: Animal
    type AnimalC = Cat
  }

  val ac = new AnimalCollection
  val dog: ac.AnimalType = ???

//  val cat: ac.BoundedAnimal = new Cat

  val pup: ac.SuperBoundedAnimal = new Dog
  val cat: ac.AnimalC = new Cat

  type CatAlias = Cat
  val anotherCat: CatAlias = new Cat

  // Alternative to Generics
  trait MyList {
    type T
    def add(element: T): MyList
  }

  class NonEmptyList(value: Int) extends MyList {
    override type T = Int
    override def add(element: Int): MyList = ???
  }

  // Dot Type
  type CatsType = cat.type
  val newCat: CatsType = cat
//  new CatsType // // class type required but lectures.part5ts.TypeMembers.cat.type found

  /*
  * Exercise - enforce a type to be applicable to SOME TYPES only
  * */
  // LOCKED
  trait MList {
    type A
    def head: A
    def tail: MList
  }

  trait ApplicableToNumbers {
    type A <: Number
  }

  // NOT OK
//  class CustomList(head: String, tail: CustomList) extends MList with ApplicableToNumbers {
//    override type A = String
//    override def head: String = head
//    override def tail: MList = tail
//  }

  // OK
//  class IntList(hd: Int, tl: IntList) extends MList with ApplicableToNumbers {
//    override type A = Int
//    override def head: Int = hd
//    override def tail: IntList = tl
//  }

  // Number
}

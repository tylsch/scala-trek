package com.rockthejvm

object ObjectOrientation extends App {
  // class and instance
  class Animal {
    val age: Int = 0
    def eat(): Unit = println("I am eating")
  }
  val anAnimal = new Animal

  //inheritance
  class Dog(val name: String) extends Animal // constructor definition
  val aDog = new Dog("Lassie")
  // constructor arguments are NOT fields, need to put a val before argument

  // subtype polymorphism
  val aDeclaredAnimal: Animal = new Dog("Hachi")
  aDeclaredAnimal.eat() // the most derived method will be called at runtime

  // abstract class
  abstract class WalkingAnimal {
    val hasLegs = true // by default public, can restrict by using private or protected
    def walk(): Unit
  }

  // "interface" = ultimate abstract type
  trait Carnivore {
    def eat(animal: Animal): Unit
  }

  trait Philosopher {
    def ?!(thought: String): Unit //valid method name
  }
  // single-class inheritance, multi-trait "mixing"
  class Crocodile extends Animal with Carnivore with Philosopher {
    override def eat(animal: Animal): Unit = println("I am eating you, animal")

    override def ?!(thought: String): Unit = println(s"I was thinking: $thought")
  }

  val aCroc = new Crocodile
  aCroc.eat(aDog)
  aCroc eat aDog // infix notation = object method argument, only available for methods with one argument
  aCroc ?! "What if we could fly?"

  // operators in Scala are actually methods
  val basicMath = 1 + 2
  val anotherBasicMath = 1.+(2) //equivalent

  // anonymous classes
  val dinosaur = new Carnivore {
    override def eat(animal: Animal): Unit = println("I am a dinosaur so I can eat pretty much anything")
  }

  // Singleton object
  object MySingleton {
    val mySpecialValue = 2345
    def mySpecialMethod(): Int = 4567
    def apply(x: Int): Int = x + 1
  } // the only instance of the MySingleton type

  MySingleton.mySpecialMethod()
  MySingleton.apply(65)
  MySingleton(65) //equivalent to apply()

  object Animal {
    // companion object, can access each others private fields/methods
    // singleton Animal and instances of Animal are different things
    val canLiveIndefinitely = false
  }

  val animalsCanLiveForever = Animal.canLiveIndefinitely // "static" fields/methods

  // case classes = lightweight data structures with some boilerplate
  // - sensible equals and hash code
  // - serialization
  // - companion with apply
  case class Person(name: String, age: Int)

  // may be constructed without new
  val bob = Person("Bob", 54)

  // exceptions
  try {
    val x: String = null
    x.length
  } catch {
    case e: Exception => s"Some faulty error message: $e"
  } finally {
    // execute code no matter what
  }

  // generics
  abstract class MyList[T] {
    def head: T
    def tail: T
  }

  // using a generic with a concrete type
  val aList: List[Int] = List(1,2,3)
  val first = aList.head
  val rest = aList.tail
  val aStringList = List("Hello", "Scala")
  val firstString = aStringList.head // string

  // Point #1: Scala we operate with IMMUTABLE values/objects
  // Any modification to an object must return ANOTHER object
  /*
  * Benefits:
  * 1) works miracles in multithreaded/distributed environments
  * 2) helps making sense of the code ("reasoning about")
  * */
  val reversedList = aList.reverse // returns a NEW list

  // Point #2: Scala is closest to the object oriented ideal
}

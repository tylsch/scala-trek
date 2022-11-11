package com.rockthejvm.part1changes

object MinorChanges {
  /*
  * Imports
  * */

  // import everything
  import scala.concurrent.duration.* // * instead of _
  // alias
  import java.util.{List => JList} // Scala 2
  import java.util.List as JList // Scala 3
  // import everything BUT something
  import java.util.{List as _ /* <-- import to be ignored */, *}

  /*
  * varargs
  * */
  val aList: List[Int] = List(1,2,3,4)
  val anArray: Array[Int] = Array(aList: _*) // Scala 2
  val anArray_v3: Array[Int] = Array(aList*) // Scala 3

  /*
  * Trait Constructor Arguments
  * */
  trait Person(name: String) // legal now
  // solve the diamond problem
//  trait JPerson extends Person("John")
//  trait APerson extends Person("Alice")
//  trait Kid extends JPerson with APerson

  /*
  * "Universal constructors", == apply methods everywhere
  * */
  class Pet(name: String)
  val lassie: Pet = Pet("Lassie") // ok

}

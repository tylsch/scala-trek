package com.rockthejvm.part2additions

import scala.annotation.targetName

object InfixNotation {

  case class Person(name: String) {
    infix /* <-- Scala 3 only */ def likes(movie: String): String = s"$name likes $movie"
    @targetName("amazedBy") // should be alphanumeric token
    infix def !!(observation: String): String = s"Wow! $observation"
  }

  // Scala 2
  val person: Person = Person("Alice")
  person.likes("Forrest Gump")
  person likes "Forrest Gump" // infix notation - for methods with ONE argument

  // Scala 3 - explicit with the "infix" modifier, infix is a SOFT modifier (not mandatory + does not collide with val names)
  val infix = 2

  // extension methods can also by infix
  extension (person: Person)
    infix def enjoys(musicGenre: String): String = s"${person.name} listens to $musicGenre"

  person enjoys "Classical music"

  /*
  * Target Name - rename "operator" methods to Java-legal names
  * - new method name can be called from Java
  * - new method name cannot be called from Scala (rather use the original name)
  * */

  // infix types
  infix trait <:>[A, B]
  type Ints = Int <:> Int

  def main(args: Array[String]): Unit = {
    person.!!("Scala!!!")
  }
}

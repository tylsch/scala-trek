package com.rockthejvm.part2additions

object OpaqueTypes {

  object SocialNetwork {
    // Some data structures = "domain"
    opaque type Name = String

    object Name {
      def apply(string: String): Name = string
    }

    extension (name: Name)
      def length: Int = name.length // use String API

    // inside, Name <-> String
    def addFriend(personal: Name, person2: Name): Boolean =
      personal.length == person2.length // use the entire String API
  }

  // outside the SocialNetwork, Name and String are NOT related
  import SocialNetwork.*
  //val name: Name = "Daniel" // will not compile

  // why: you don't need (or want) to have access to the entire String API for the Name type

  object Graphics {
    opaque type Color = Int // in hex
    opaque type ColorFilter <: Color = Int

    val Red: Color = 0xFF000000
    val Green: Color = 0x00FF0000
    val Blue: Color = 0x0000FF00
    val halfTransparency: ColorFilter = 0x80
  }

  import Graphics.*
  case class OverlayFilter(color: Color)

  val fadeLayer: OverlayFilter = OverlayFilter(halfTransparency) // ColorFilter <: Color

  // how can we create instances of opaque types + how to access their APIs
  // 1 - companion objects
  val aName: Name = Name("Daniel") // ok
  // 2 - extension methods
  val nameLength: Int = aName.length // ok

  def main(args: Array[String]): Unit = {

  }
}

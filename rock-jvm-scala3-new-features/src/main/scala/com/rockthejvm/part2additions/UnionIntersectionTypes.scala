package com.rockthejvm.part2additions

object UnionIntersectionTypes {

  // union types
  val truthOr42: Boolean | Int = 42

  def ambivalentMethod(arg: String | Int): String = arg match
    case _: String => "a String"
    case _: Int => "a number"

  val aNumberDescription: String = ambivalentMethod(55)
  val aStringDescription: String = ambivalentMethod("Scala")

  // type inference will do Lowest Common Ancestor of the branches
  val stringOrInt: Any = if (42 > 0) "a String" else 42
  val stringOrInt_v2: String | Int = if (42 > 0) "a String" else 42 // ok if we pass teh union type explicitly

  // "flow" typing = explicit null checking
  type Maybe[T] = T | Null
  def handleMaybe(someValue: Maybe[String]): Int =
    if (someValue != null) someValue.length
    else 0

  // Flow typing is restricted
//  type ErrorOr[T] = T | "error"
//  def handleResource(arg: ErrorOr[Int]): Unit =
//    if (arg != "error") println(arg + 1) // does not work
//    else println("error")

  // intersection types
  class Animal
  trait Carnivore
  class Crocodile extends Animal with Carnivore

  val carnivoreAnimal: Animal & Carnivore = new Crocodile

  // intersection types & the diamond problem
  trait Gadget {
    def use(): Unit
  }
  trait Camera extends Gadget:
    def takePicture(): Unit = println("smile!")
    override def use(): Unit = println("snap!")

  trait Phone extends Gadget:
    def makePhoneCall(): Unit = println("Dialing")
    override def use(): Unit = println("ring ring")

  def useSmartDevice(cameraPhone: Camera & Phone): Unit =
    cameraPhone.takePicture()
    cameraPhone.makePhoneCall()
    cameraPhone.use() // ??? depends on how Camera and Phone are inherited (order) - trait linearization

  class SmartPhone extends Phone with Camera // use == snap
  class CameraWithPhone extends Camera with Phone // use == ring ring

  // intersection types + covariance
  trait HostConfig
  trait HostController:
    def get: Option[HostConfig]

  trait PortConfig
  trait PortController:
    def get: Option[PortConfig]

  // works/compiles because Option is covariant
  def getConfigs(controller: HostController & PortController): Option[HostConfig & PortConfig] = controller.get



  def main(args: Array[String]): Unit = {
    useSmartDevice(new SmartPhone)
    useSmartDevice(new CameraWithPhone)
  }
}

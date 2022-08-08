package lectures.part5ts

object StructuralTypes extends App {
  // structural types
  type JavaClosable = java.io.Closeable

  class HipsterCloseable {
    def close(): Unit = println("yeah yeah I'm closing")
    def closeSilently(): Unit = println("not making a sound")
  }

//  def closeQuietly(closable: JavaClosable OR HipsterCloseable) // ???

  type UnifiedCloseable = {
    def close(): Unit
  } // STRUCTURAL TYPE

  def closeQuietly(closeable: UnifiedCloseable): Unit = closeable.close()
  closeQuietly(new JavaClosable {
    override def close(): Unit = ???
  })
  closeQuietly(new HipsterCloseable)

  // TYPE REFINEMENTS
  type AdvancedCloseble = JavaClosable {
    def closeSilently(): Unit
  }

  class AdvanceJavaCloseable extends JavaClosable {
    override def close(): Unit = println("Java closes")
    def closeSilently(): Unit = println("Java closes silently")
  }

  def closeShh(advancedCloseble: AdvancedCloseble): Unit = advancedCloseble.closeSilently()

  closeShh(new AdvanceJavaCloseable)
//  closeShh(new HipsterCloseable)

  // using structural types as standalone types
  def altClose(closeable: {def close(): Unit }): Unit = closeable.close()

  // type-checking => duck typing
  type SoundMaker = {
    def makeSound(): Unit
  }

  class Dog {
    def makeSound(): Unit = println("Bark")
  }

  class Car {
    def makeSound(): Unit = println("Vroom")
  }

  val dog: SoundMaker = new Dog
  val car: SoundMaker = new Car

  // static duck typing
  // CAVEAT: based on reflection

  trait CBL[+T] {
    def head: T
    def tail: CBL[T]
  }

  class Human {
    def head: Brain = new Brain
  }
  class Brain {
    override def toString: String = "BRAINZ!!!"
  }

  def f[T](somethingWithAHead: { def head: T }): Unit = println(somethingWithAHead.head)

  case object CBNil extends CBL[Nothing] {
    override def head: Nothing = ???
    override def tail: CBL[Nothing] = ???
  }
  case class CBCons[T](override val head: T, override val tail: CBL[T]) extends CBL[T]

  f(CBCons(2, CBNil))
  f(new Human)

  object HeadEqualizer {
    type Headable[T] = { def head: T }
    def ===[T](a: Headable[T], b: Headable[T]): Boolean = a.head == b.head
  }

  val brainsList = CBCons(new Brain, CBNil)
  val stringsList = CBCons("BRAINZ", CBNil)
  HeadEqualizer.===(brainsList, new Human)
  // problem
  HeadEqualizer.===(new Human, stringsList) // not type safe
}

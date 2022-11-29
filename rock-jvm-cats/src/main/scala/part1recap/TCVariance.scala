package part1recap

object TCVariance {

  import cats.Eq
  import cats.instances.int._
  import cats.instances.option._
  import cats.syntax.eq._

  val aComparison = Option(2) === Option(3)
  // val anInvalidComparison = Some(2) === None // Eq[Some[Int]] not found

  // variance
  class Animal
  class Cat extends Animal

  // covariant type: subtyping is propagated to the generic type
  class Cage[+T]
  val cage: Cage[Animal] = new Cage[Cat] // Cat <: Animal, so Cage[Cat] <: Cage[Animal]

  // contravariant type: subtyping is propagated BACKWARDS to the generic type
  class Vet[-T]
  val vet: Vet[Cat] = new Vet[Animal] // Cat <: Animal, so Vet[Animal] <: Vet[Cat]

  // rule of thumb: "HAS a T" = covariant, "ACTS on T" = contravariant
  // variance affect how type class instances are being fetched

  trait SoundMaker[-T]
  implicit object AnimalSoundMaker extends SoundMaker[Animal]
  def makeSound[T](implicit soundMaker: SoundMaker[T]): Unit = println("Wow")
  makeSound[Animal] // ok - type class defined above
  makeSound[Cat] // ok = type class instance for Animal is also applicable to Cats
  // rule 1: contravariant type classes can use the superclass instances if nothing is available strictly for that type

  // has implications for subtypes
  implicit object OptionSoundMaker extends SoundMaker[Option[Int]]
  //makeSound(Option[Int])
  //makeSound(Some[Int])

  // covariant type class
  trait AnimalShow[+T] {
    def show: String
  }
  implicit object GeneralAnimalShow extends AnimalShow[Animal] {
    override def show: String = "Animals Everywhere"
  }
  implicit object CatsShow extends AnimalShow[Cat] {
    override def show: String = "Cats Everywhere"
  }
  def organizeShow[T](implicit event: AnimalShow[T]): String = event.show
  // rule 2: covariant type classes will always use the more specific type class instance for that type
  // but may confuse the compiler if the general type class is also present

  // rule 3: you can't have both benefits
  // cats uses INVARIANT type classes
  Option(2) === Option.empty[Int]


  def main(args: Array[String]): Unit = {
    println(organizeShow[Cat]) // ok - the compile will inject CatsShow as implicit
    //println(organizeShow[Animal]) // will not compile - ambiguous values
  }
}

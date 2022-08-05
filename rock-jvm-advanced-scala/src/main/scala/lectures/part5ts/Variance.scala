package lectures.part5ts

object Variance extends App {
  trait Animal
  class Dog extends Animal
  class Cat extends Animal
  class Crocodile extends Animal

  // what is variance???
  // "inheritance" - type substitution of generics

  class Cage[T]
  // yes - covariance
  class CCage[+T]
  val ccage: CCage[Animal] = new CCage[Cat]

  // no - invariance
  class ICage[T]
//  val icage: ICage[Animal] = new ICage[Cat]

  // hell no - opposite = contravariance
  class XCage[-T]
  val xcage: XCage[Cat] = new XCage[Animal]

  class InvariantCage[T](val animal: T) // Invariant

  // covariant positions
  class CovariantCage[+T](val animal: T) // Covariant position

//  class ContravariantCage[-T](val animal: T) // Contravariant type T occurs in covariant position in type T of value animal
  /*
  * val catCage: XCage[Cat] = new XCage[Animal](new Crocodile)
  * */

//  class CovariantVariableCage[+T](var animal: T) // Covariant type T occurs in contravariant position in type T of value animal
  /*
  * val ccage: CCage[Animal] = new CCage[Cat](new Cat)
  * ccage.animal = new Crocodile
  * */
//  class ContravariantVariableCage[-T](var animal: T) //Contravariant type T occurs in covariant position in type T of value animal
  class InvariantVariableCage[T](var animal: T) // ok

//  trait AnotherCovariantCage[+T] {
//    def addAnimal(animal: T) // CONTRAVARIANT POSITION
//  }
  /*
  * val ccage: CCage[Animal] = new CCage[Dog]
  * ccage.add(new Cat)
  * */

  class AnotherContravariantCage[-T] {
    def addAnimal(animal: T) = true
  }
  val acc: AnotherContravariantCage[Cat] = new AnotherContravariantCage[Animal]
  acc.addAnimal(new Cat)
  class Kitty extends Cat
  acc.addAnimal(new Kitty)

  class MyList[+A] {
    def add[B >: A](element: B): MyList[B] = new MyList[B] // widening the type
  }

  val empty = new MyList[Kitty]
  val animals = empty.add(new Kitty)
  val moreAnimals = animals.add(new Cat)
  val evenMoreAnimals = moreAnimals.add(new Dog)

  // Method Arguments are in Contravariant position!!!

  // return types
  class PetShop[-T] {
//    def get(isItAPuppy: Boolean): T // METHOD RETURN TYPES ARE IN COVARIANT POSITION
    def get[S <: T](isItAPuppy: Boolean, defaultAnimal: S): S = defaultAnimal
  }

  val shop: PetShop[Dog] = new PetShop[Animal]
//  val evilCat = shop.get(true, new Cat)
  class TerraNova extends Dog
  val bigFurry = shop.get(true, new TerraNova)
  /*
  * BIG RULE
  * - method arguments are in CONTRAVARIANT POSITION
  * - return types are in COVARIANT POSITION
  * */

  /*
  * 1. Invariant, Covariant, Contravariant Parking[T](things: List[T])
  * - park(vehicle: T)
  * - impound(vehicles: List[T])
  * - checkVehicles(conditions: String): List[T]
  *
  * 2. Used someone else's API: IList[T]
  * 3. Parking = monad
  *   - flatMap
  * */
  class Vehicle
  class Bike extends Vehicle
  class Car extends Vehicle
  class IList[T]

  class IParking[T](vehicles: List[T]) {
    def park(vehicle: T): IParking[T] = ???
    def impound(vehicles: List[T]): IParking[T] = ???
    def checkVehicles(conditions: String): List[T] = ???

    def flatMap[S](f: T => IParking[S]): IParking[S] = ???
  }

  class CParking[+T](vehicles: List[T]) {
    def park[S >: T](vehicle: S): CParking[S] = ???
    def impound[S >: T](vehicles: List[S]): CParking[S] = ???
    def checkVehicles(conditions: String): List[T] = ???

    def flatMap[S](f: T => CParking[S]): CParking[S] = ???
  }

  class XParking[-T](vehicles: List[T]) {
    def park(vehicle: T): XParking[T] = ???
    def impound(vehicles: List[T]): XParking[T] = ???
    def checkVehicles[S <: T](conditions: String): List[S] = ???

    def flatMap[R <: T, S](f: R => XParking[S]): XParking[S] = ???
  }

  /*
  * rule of thumb
  * - use covariance = COLLECTION OF THINGS
  * - use contravariance = GROUP OF ACTIONS
  * */

  class CParking2[+T](vehicles: IList[T]) {
    def park[S >: T](vehicle: S): CParking2[S] = ???
    def impound[S >: T](vehicles: IList[S]): CParking2[S] = ???
    def checkVehicles[S >: T](conditions: String): IList[S] = ???
  }

  class XParking2[-T](vehicles: IList[T]) {
    def park(vehicle: T): XParking2[T] = ???
    def impound[S <: T](vehicles: IList[S]): XParking2[S] = ???
    def checkVehicles[S <: T](conditions: String): List[S] = ???
  }

  // flatMap
}

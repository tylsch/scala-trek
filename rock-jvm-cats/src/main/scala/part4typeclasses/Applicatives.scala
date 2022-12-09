package part4typeclasses

object Applicatives {

  import cats.Applicative
  import cats.instances.list._
  val listApplicative = Applicative[List]
  val aList = listApplicative.pure(2) // List(2)

  import cats.instances.option._
  val optionApplicative = Applicative[Option]
  val anOption = optionApplicative.pure(2) // Some(2)

  // pure extension method
  import cats.syntax.applicative._
  val aSweetList = 2.pure[List]
  val aSweetOption = 2.pure[Option]

  // Monads extend Applicatives
  // Applicatives extend Functors
  import cats.data.Validated
  type ErrorsOr[T] = Validated[List[String], T]
  val aValidValue = Validated.valid(43) // "pure"
  val aModifiedValidated: ErrorsOr[Int] = aValidValue.map(_ + 1) // map
  val validatedApplicative = Applicative[ErrorsOr]

  //def ap[W[_], B, T](ff: W[B => T])(fa: W[B]): W[T] = ??? // this already implemented

  def productWithApplicatives[W[_], A, B](wa: W[A], wb: W[B])(implicit applicative: Applicative[W]): W[(A, B)] = {
    val functionWrapper: W[B => (A, B)] = applicative.map(wa)(a => (b: B) => (a, b))
    applicative.ap(functionWrapper)(wb)
  }

  // Applicatives have this ap()
  // Applicatives can implement product from semigroupals
  // => Applicatives extend Semigroupals

  def main(args: Array[String]): Unit = {

  }
}

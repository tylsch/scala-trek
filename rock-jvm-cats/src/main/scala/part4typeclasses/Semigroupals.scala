package part4typeclasses

import cats.Monad

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}

object Semigroupals {

  trait MySemigroupal[F[_]] {
    def product[A, B](fa: F[A], fb: F[B]): F[(A, B)]
  }

  import cats.Semigroupal
  import cats.instances.option._
  val optionSemigroupal = Semigroupal[Option]
  val aTupleOption: Option[(Int, String)] = optionSemigroupal.product(Some(123), Some("a String"))
  val aNoneTupled: Option[(Int, Nothing)] = optionSemigroupal.product(Some(123), None) // None

  import cats.instances.future._
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(8))
  val aTupledFuture = Semigroupal[Future].product(Future("the meaning of life"), Future(42))

  import cats.instances.list._ // Monad[List]
  val aTupledList = Semigroupal[List].product(List(1,2), List("a", "b"))

  import cats.syntax.functor._
  import cats.syntax.flatMap._
  def productWithMonads[F[_] : Monad, A, B](fa: F[A], fb: F[B]): F[(A, B)] =
    for {
      a <- fa
      b <- fb
    } yield (a, b)

  // MONADS EXTEND SEMIGROUPALS!!!
  // example: Validated
  import cats.data.Validated
  type ErrorsOr[T] = Validated[List[String], T]
  val validatedSemigroupal = Semigroupal[ErrorsOr] // requires teh implicit Semigroup[List[_]]

  val invalidsCombination = validatedSemigroupal.product(
    Validated.invalid(List("Something wrong", "Something else wrong")),
    Validated.invalid(List("this can't be right"))
  )

  type EitherErrorsOr[T] = Either[List[String], T]
  import cats.instances.either._
  val eitherSemigroupal = Semigroupal[EitherErrorsOr]
  val eitherCombination = eitherSemigroupal.product(
    Left(List("Something wrong", "Something else wrong")),
    Left(List("this can't be right"))
  )

  val zipListSemigroupal: Semigroupal[List] = new Semigroupal[List] {
    override def product[A, B](fa: List[A], fb: List[B]): List[(A, B)] = fa.zip(fb)
  }

  def main(args: Array[String]): Unit = {
    println(aTupledList)
    println(invalidsCombination)
    println(eitherCombination)
    println(zipListSemigroupal.product(List(1,2), List("a", "b")))
  }
}

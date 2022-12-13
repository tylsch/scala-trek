package part4typeclasses

import cats.{Applicative, Monad}

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object HandlingErrors {

  trait MyApplicativeError[M[_], E] extends Applicative[M] {
    def raiseError[A](e: E): M[A]
    def handleErrorWith[A](ma: M[A])(func: E => M[A]): M[A]
    def handleError[A](ma: M[A])(func: E => M[A]): M[A] = handleErrorWith(ma)(e => pure(func(e)))
  }
  trait MyMonadError[M[_], E] extends MyApplicativeError[M , E] with Monad[M] {
    def ensure[A](ma: M[A])(error: E)(predicate: A => Boolean): M[A]
  }

  import cats.MonadError
  import cats.instances.either._
  type ErrorOr[A] = Either[String, A]
  val monadErrorEither = MonadError[ErrorOr, String]
  val success = monadErrorEither.pure(32) // Either[String, Int] == Right(32)
  val failure = monadErrorEither.raiseError[Int]("Something Wrong") // Either[String, Int] == Left("Something Wrong")
  // recover
  val handledError: ErrorOr[Int] = monadErrorEither.handleError(failure) {
    case "Badness" => 44
    case _ => 89
  }
  // recoverWith
  val handledError2: ErrorOr[Int] = monadErrorEither.handleErrorWith(failure) {
    case "Badness" => monadErrorEither.pure(44)
    case _ => Left("Something else")
  }
  // filter
  val filteredSuccess = monadErrorEither.ensure(success)("Number too small")(_ > 100)

  // Try and Future
  import cats.instances.try_._
  val exception = new RuntimeException("Really bad")
  val pureException: Try[Int] = MonadError[Try, Throwable].raiseError(exception) // Failure(exception)
  import cats.instances.future._
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(8))
  val pureFuture = MonadError[Future, Throwable].raiseError(exception) // Future will complete with a Failure(exception)

  // Applicatives => ApplicativeError
  import cats.data.Validated
  import cats.instances.list._

  type ErrorsOr[T] = Validated[List[String], T]
  import cats.ApplicativeError
  val applicativeError = ApplicativeError[ErrorsOr, List[String]]
  //pure, raiseError, handleError, handleErrorWith

  // extension methods
  import cats.syntax.applicative._
  import cats.syntax.applicativeError._
  val extendedSuccess: ErrorsOr[Int] = 42.pure[ErrorsOr] // requires the implicit ApplicativeError[ErrorsOr, List[String]]
  val extendedError: ErrorsOr[Int] = List("Bad").raiseError[ErrorsOr, Int]
  val recoveredError: ErrorsOr[Int] = extendedError.recover {
    case _ => 43
  }

  import cats.syntax.monadError._ // ensure
  val testedSuccess: ErrorOr[Int] = success.ensure("Something bad")(_ > 100)

  def main(args: Array[String]): Unit = {

  }
}

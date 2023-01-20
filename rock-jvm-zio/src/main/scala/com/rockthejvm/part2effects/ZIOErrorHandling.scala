package com.rockthejvm.part2effects

import zio.{IO, Task, URIO, ZIO, ZIOAppDefault}

import scala.util.{Failure, Success, Try}

object ZIOErrorHandling extends ZIOAppDefault {

  // ZIOs can fail
  val aFailedZio = ZIO.fail("Something went wrong")
  val failedWithThrowable = ZIO.fail(new RuntimeException("Boom!"))
  val failedWithDescription = failedWithThrowable.mapError(_.getMessage)

  // attempt: run an effect that might throw an exception
  val badZIO = ZIO.succeed {
    println("Trying something")
    val string: String = null
    string.length
  } // this is bad

  // use attempt if you're every unsure whether your code might throw
  val anAttempt: Task[Int] = ZIO.attempt {
    println("Trying something")
    val string: String = null
    string.length
  }

  // effectually catch error
  val catchError = anAttempt.catchAll(e => ZIO.succeed(s"Returning a different value because ${e}"))
  val catchSelectiveErrors = anAttempt.catchSome {
    case e: RuntimeException => ZIO.succeed(s"Ignoring runtime exceptions: ${e}")
    case _ => ZIO.succeed("Ignoring everything else")
  }

  // chain effects
  val aBetterAttempt = anAttempt.orElse(ZIO.succeed(56))
  // fold: handle both success and failure
  val handleBoth: URIO[Any, String] = anAttempt.fold(ex => s"Something bad happened: $ex", value => s"Length of the string was $value")
  // effectful fold: foldZIO
  val handleBoth_v2 = anAttempt.foldZIO(
    ex => ZIO.succeed(s"Something bad happened: $ex"),
    value => ZIO.succeed(s"Length of the string was $value")
  )

  /*
  * Conversions between Option/Try/Either to ZIO
  *
  * */

  val aTryZIO: Task[Int] = ZIO.fromTry(Try(42 / 0)) // can fail with Throwable

  // either -> ZIO
  val anEither: Either[Int, String] = Right("Success!")
  val anEitherToZIO = ZIO.fromEither(anEither)
  // ZIO -> ZIO with Either as the value channel
  val eitherZIO = anAttempt.either
  // reverse
  val anAttempt_v2 = eitherZIO.absolve

  // option -> ZIO
  val anOption: ZIO[Any, Option[Nothing], Int] = ZIO.fromOption(Some(42))

  def try2ZIO[A](aTry: Try[A]): Task[A] = aTry match
    case Failure(exception) => ZIO.fail(exception)
    case Success(value) => ZIO.succeed(value)

  def either2ZIO[A, B](anEither: Either[A, B]): ZIO[Any, A, B] = anEither match
    case Left(value) => ZIO.fail(value)
    case Right(value) => ZIO.succeed(value)

  def option2ZIO[A](anOption: Option[A]): ZIO[Any, Option[A], A] = anOption match
    case Some(value) => ZIO.succeed(value)
    case None => ZIO.fail(None)

  def zio2zioEither[R, A, B](zio: ZIO[R, A, B]): ZIO[R, Nothing, Either[A, B]] = zio.foldZIO(
    error => ZIO.succeed(Left(error)),
    value => ZIO.succeed(Right(value))
  )

  def absolveZio[R, A, B](zio: ZIO[R, Nothing, Either[A, B]]): ZIO[R, A, B] = zio.flatMap {
    case Left(value) => ZIO.fail(value)
    case Right(value) => ZIO.succeed(value)
  }

  override def run: ZIO[Any, Any, Any] = ???
}

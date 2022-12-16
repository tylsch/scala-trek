package com.rockthejvm.part2effects

import cats.effect.IO

import scala.util.{Failure, Success, Try}

object IOErrorHandling {

  // IO: pure, delay, defer
  // create failed effects
  val aFailedCompute: IO[Int] = IO.delay(throw new RuntimeException("A failure"))
  val aFailure: IO[Int] = IO.raiseError(new RuntimeException("A proper failure"))

  // handle exceptions
  val dealWithIt = aFailure.handleErrorWith {
    case _: RuntimeException => IO.delay(println("I'm still here"))
    // add more cases
  }

  // turn into an Either
  val effectAsEither: IO[Either[Throwable, Int]] = aFailure.attempt
  // redeem: transform the failure and the success in one go
  val resultAsString: IO[String] = aFailure.redeem(ex => s"FAIL: $ex", value => s"SUCCESS: $value")
  // redeemWith
  val resultAsEffect: IO[Unit] = aFailure.redeemWith(ex => IO(println(s"FAIL: $ex")), value => IO(println(s"SUCCESS: $value")))

  def option2IO[A](option: Option[A])(ifEmpty: Throwable): IO[A] =
    option match
      case Some(value) => IO.pure(value)
      case None => IO.raiseError(ifEmpty)

  def try2IO[A](aTry: Try[A]): IO[A] =
    aTry match
      case Success(value) => IO.pure(value)
      case Failure(exception) => IO.raiseError(exception)

  def either2IO[A](either: Either[Throwable, A]): IO[A] =
    either match
      case Left(ex) => IO.raiseError(ex)
      case Right(value) => IO.pure(value)

  def handleIOError[A](io: IO[A])(handler: Throwable => A): IO[A] =
    io.redeem(handler, identity)

  def handleIOErrorWith[A](io: IO[A])(handler: Throwable => IO[A]): IO[A] =
    io.redeemWith(handler, value => IO.pure(value))

  def main(args: Array[String]): Unit = {
    import cats.effect.unsafe.implicits.global
    println(resultAsString.unsafeRunSync())
    resultAsEffect.unsafeRunSync()
  }
}

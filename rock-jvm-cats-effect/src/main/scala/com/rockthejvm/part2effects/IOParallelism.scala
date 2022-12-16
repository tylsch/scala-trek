package com.rockthejvm.part2effects

import cats.Parallel
import cats.effect.IO.Par
import cats.effect.{IO, IOApp}

object IOParallelism extends IOApp.Simple {

  // IOs are usually sequential
  val anisIO = IO(s"[${Thread.currentThread().getName}] Ani")
  val kamranIO = IO(s"[${Thread.currentThread().getName}] Kamran")

  val composedIO = for {
    ani <- anisIO
    kamran <- kamranIO
  } yield s"$ani and $kamran love Rock the JVM"

  // debug extension method
  import com.rockthejvm.utils._
  import cats.syntax.apply._
  val meaningOfLife: IO[Int] = IO.delay(42)
  val favoriteLanguage: IO[String] = IO.delay("Scala")
  val goalInLife = (meaningOfLife.debugM, favoriteLanguage.debugM).mapN((num, string) => s"My goal in life is $num and $string")

  // convert a sequential IO to parallel IO
  val parIO1: IO.Par[Int] = Parallel[IO].parallel(meaningOfLife.debugM)
  val parIO2: IO.Par[String] = Parallel[IO].parallel(favoriteLanguage.debugM)
  import cats.effect.implicits._
  val goalInLifeP: Par[String] = (parIO1, parIO2).mapN((num, string) => s"My goal in life is $num and $string")
  // turn back to sequential
  val goalInLife_v2: IO[String] = Parallel[IO].sequential(goalInLifeP)

  // shorthand:
  import cats.syntax.parallel._
  val goalInLife_v3: IO[String] = (meaningOfLife.debugM, favoriteLanguage.debugM).parMapN((num, string) => s"My goal in life is $num and $string")

  // regarding failure:
  val aFailure: IO[String] = IO.raiseError(new RuntimeException("I can't do this"))
  // compose success + failure
  val parallelWithFailure = (meaningOfLife.debugM, aFailure.debugM).parMapN(_ + _)
  // compose a failure with failure
  val anotherFailure: IO[String] = IO.raiseError(new RuntimeException("I can't do this 2"))
  val twoFailures: IO[String] = (aFailure.debugM, anotherFailure.debugM).parMapN(_ + _)
  // the first effect to fail gives the failure of the result
  val twoFailuresDelayed: IO[String] = (IO(Thread.sleep(1000)) >> aFailure.debugM, anotherFailure.debugM).parMapN(_ + _)

  override def run: IO[Unit] =
    twoFailuresDelayed.debugM.void
}

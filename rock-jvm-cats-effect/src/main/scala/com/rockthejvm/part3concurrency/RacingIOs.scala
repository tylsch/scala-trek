package com.rockthejvm.part3concurrency

import cats.effect.kernel.Outcome.{Canceled, Errored, Succeeded}
import cats.effect.{FiberIO, IO, IOApp, OutcomeIO}

import scala.concurrent.duration.*

object RacingIOs extends IOApp.Simple {
  import com.rockthejvm.utils._
  def runWithSleep[A](value: A, duration: FiniteDuration): IO[A] =
    (
      IO(s"starting computation: $value").debugM >>
      IO.sleep(duration) >>
      IO(s"computation for $value: done") >>
      IO(value)
    ).onCancel(IO(s"computation CANCELLED for $value").debugM.void)

  def testRace() = {
    val meaningOfLife = runWithSleep(42, 1.second)
    val favLang = runWithSleep("Scala", 2.seconds)
    val first: IO[Either[Int, String]] = IO.race(meaningOfLife, favLang)
    /*
    * both IOs run on separate fibers
    * - teh first one to finish will complete the result
    * - the loser will be canceled
    * */

    first.flatMap {
      case Left(value) => IO(s"Meaning of life won: $value")
      case Right(value) => IO(s"Fav language won: $value")
    }
  }

  def testRacePair() = {
    val meaningOfLife = runWithSleep(42, 1.second)
    val favLang = runWithSleep("Scala", 2.seconds)
    val raceResult: IO[Either[(OutcomeIO[Int], FiberIO[String]), (FiberIO[Int], OutcomeIO[String])]] = IO.racePair(meaningOfLife, favLang)

    raceResult.flatMap {
      case Left((outMol, fibLang)) => fibLang.cancel >> IO("MOL Won").debugM >> IO(outMol).debugM
      case Right((fibMol, outLang)) => fibMol.cancel >> IO("Language Won").debugM >> IO(outLang).debugM
    }
  }

  def timeout[A](io: IO[A], duration: FiniteDuration): IO[A] =
    val result: IO[Either[A, Unit]] = IO.race(io, IO.sleep(duration))

    result.flatMap {
      case Left(value) => IO(value)
      case Right(_) => IO.raiseError(new RuntimeException("Computation timed out."))
    }

  val importantTask = IO.sleep(2.seconds) >> IO(42).debugM
  val testTimeout = timeout(importantTask, 3.second)
  val testTimeout_v2 = importantTask.timeout(1.second)

  def unrace[A, B](ioa: IO[A], iob: IO[B]): IO[Either[A, B]] =
    IO.racePair(ioa, iob).flatMap {
      case Left((_, fibB)) => fibB.join.flatMap {
        case Succeeded(fa) => fa.map(result => Right(result))
        case Errored(e) => IO.raiseError(e)
        case Canceled() => IO.raiseError(new RuntimeException("Loser cancelled"))
      }
      case Right((fibA, _)) => fibA.join.flatMap {
        case Succeeded(fa) => fa.map(result => Left(result))
        case Errored(e) => IO.raiseError(e)
        case Canceled() => IO.raiseError(new RuntimeException("Loser cancelled"))
      }
    }

  def simpleRace[A, B](ioa: IO[A], iob: IO[B]): IO[Either[A, B]] =
    IO.racePair(ioa, iob).flatMap {
      case Left((outA, fibB)) => outA match
        case Succeeded(fa) => fibB.cancel >> fa.map(a => Left(a))
        case Errored(e) => fibB.cancel >> IO.raiseError(e)
        case Canceled() => fibB.join.flatMap {
          case Succeeded(fb) => fb.map(b => Right(b))
          case Errored(e) => IO.raiseError(e)
          case Canceled() => IO.raiseError(new RuntimeException("Both computations cancelled"))
        }
      case Right((fibA, outB)) => outB match
        case Succeeded(fb) => fibA.cancel >> fb.map(b => Right(b))
        case Errored(e) => fibA.cancel >> IO.raiseError(e)
        case Canceled() => fibA.join.flatMap {
          case Succeeded(fa) => fa.map(a => Left(a))
          case Errored(e) => IO.raiseError(e)
          case Canceled() => IO.raiseError(new RuntimeException("Both computations cancelled"))
        }
    }

  override def run: IO[Unit] = testRace().debugM.void
}

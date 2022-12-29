package com.rockthejvm.part5polymorphic

import cats.effect.kernel.Outcome
import cats.effect.kernel.Outcome.{Canceled, Errored, Succeeded}
import cats.effect.{Fiber, IO, IOApp, MonadCancel, Spawn}

object PolymorphicFibers extends IOApp.Simple {

  // Spawn = create fibers for any effect

  trait MyGenSpawn[F[_], E] extends MonadCancel[F, E]:
    def start[A](fa: F[A]): F[Fiber[F, Throwable, A]] // creates a fiber
    def never[A]: F[A] // a forever-suspending effect
    def cede: F[Unit] // a "yield" effect

    def racePair[A, B](fa: F[A], fb: F[B]): F[Either[(Outcome[F, E, A], Fiber[F, E, B]), (Fiber[F, E, A], Outcome[F, E, B])]] // fundamental racing

  trait MySpawn[F[_]] extends MyGenSpawn[F, Throwable]

  val mol = IO(42)
  val fiber: IO[Fiber[IO, Throwable, Int]] = mol.start

  // pure, map/flatMap, raiseError, uncancelable, start

  val spawnIO = Spawn[IO] // fetch the given/implicit Spawn[IO]

  def ioOnSomeThread[A](io: IO[A]): IO[Outcome[IO, Throwable, A]] =
    for
      fib <- spawnIO.start(io) // io.start assumes the presence of a Spawn[IO]
      result <- fib.join
    yield result

  import cats.syntax.functor.*
  import cats.syntax.flatMap.*

  // generalize
  import cats.effect.syntax.spawn.*
  def effectOnSomeThread[F[_], A](fa: F[A])(using spawn: Spawn[F]): F[Outcome[F, Throwable, A]] =
    for
      fib <- fa.start
      result <- fib.join
    yield result

  val molOnFiber = ioOnSomeThread(mol)
  val molOnFiber_v2 = effectOnSomeThread(mol)

  // Exercise
  def ioRace[A, B](ioa: IO[A], iob: IO[B]): IO[Either[A, B]] =
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

  def generalRace[F[_], A, B](fa: F[A], fb: F[B])(using spawn: Spawn[F]): F[Either[A, B]] =
    spawn.racePair(fa, fb).flatMap {
      case Left((outA, fibB)) => outA match
        case Succeeded(fa) => fibB.cancel.flatMap(_ => fa.map(a => Left(a)))
        case Errored(e) => fibB.cancel.flatMap(_ => spawn.raiseError(e))
        case Canceled() => fibB.join.flatMap {
          case Succeeded(fb) => fb.map(b => Right(b))
          case Errored(e) => spawn.raiseError(e)
          case Canceled() => spawn.raiseError(new RuntimeException("Both computations cancelled"))
        }
      case Right((fibA, outB)) => outB match
        case Succeeded(fb) => fibA.cancel.flatMap(_ => fb.map(b => Right(b)))
        case Errored(e) => fibA.cancel.flatMap(_ => spawn.raiseError(e))
        case Canceled() => fibA.join.flatMap {
          case Succeeded(fa) => fa.map(a => Left(a))
          case Errored(e) => spawn.raiseError(e)
          case Canceled() => spawn.raiseError(new RuntimeException("Both computations cancelled"))
        }
    }

  import scala.concurrent.duration.*
  import com.rockthejvm.utils.general.*

  val fast = IO.sleep(1.second) >> IO(42).debugM
  val slow = IO.sleep(2.seconds) >> IO("Scala").debugM
  val race = ioRace(fast, slow)
  val race_v2 = generalRace(fast, slow)
  override def run: IO[Unit] = race_v2.void
}

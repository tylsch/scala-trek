package com.rockthejvm.part5polymorphic

import cats.effect.kernel.Outcome
import cats.effect.{Concurrent, Deferred, Fiber, IO, IOApp, Ref, Spawn}

object PolymorphicCoordination extends IOApp.Simple {

  // Concurrent = Ref + Deferred for ANY effect type
  trait MyConcurrent[F[_]] extends Spawn[F]:
    def ref[A](a: A): F[Ref[F, A]]
    def deferred[A]: F[Deferred[F, A]]

  val concurrentIO = Concurrent[IO] // given instance of Concurrent[IO]
  val aDeferred = Deferred[IO, Int] // given Concurrent[IO] in scope
  val aDeferred_v2 = concurrentIO.deferred[Int]
  val aRef = concurrentIO.ref(42)

  // capabilities: pure, map/flatMap, raiseError, uncancelable, start (fibers), + ref/deferred

  import com.rockthejvm.utils.general.*
  import scala.concurrent.duration.*
  def eggBoiler(): IO[Unit] = {
    def eggReadyNotification(signal: Deferred[IO, Unit]) =
      for {
        _ <- IO("Egg boiling on some other fiber, waiting...").debugM
        _ <- signal.get
        _ <- IO("EGG READY!!!").debugM
      } yield ()

    def tickingClock(ticks: Ref[IO, Int], signal: Deferred[IO, Unit]): IO[Unit] =
      for {
        _ <- IO.sleep(500.millis)
        count <- ticks.updateAndGet(_ + 1)
        _ <- IO(count).debugM
        _ <- if (count >= 10) signal.complete(()) else tickingClock(ticks, signal)
      } yield ()

    for {
      counter <- Ref[IO].of(0)
      signal <- Deferred[IO, Unit]
      fibNotifier <- eggReadyNotification(signal).start
      clock <- tickingClock(counter, signal).start
      _ <- fibNotifier.join
      _ <- clock.join
    } yield ()
  }

  import cats.syntax.flatMap.*
  import cats.syntax.functor.*
  import cats.effect.syntax.spawn.*
  def polymorphicEggBoiler[F[_]](using concurrent: Concurrent[F]): F[Unit] = {
    def eggReadyNotification(signal: Deferred[F, Unit]) =
      for {
        _ <- concurrent.pure("Egg boiling on some other fiber, waiting...").debugM
        _ <- signal.get
        _ <- concurrent.pure("EGG READY!!!").debugM
      } yield ()

    def tickingClock(ticks: Ref[F, Int], signal: Deferred[F, Unit]): F[Unit] =
      for {
        _ <- unsafeSleep[F, Throwable](1.second)
        count <- ticks.updateAndGet(_ + 1)
        _ <- concurrent.pure(count).debugM
        _ <- if (count >= 10) signal.complete(()).void else tickingClock(ticks, signal)
      } yield ()

    for {
      counter <- concurrent.ref(0)
      signal <- concurrent.deferred[Unit]
      fibNotifier <- eggReadyNotification(signal).start
      clock <- tickingClock(counter, signal).start
      _ <- fibNotifier.join
      _ <- clock.join
    } yield ()
  }

  // Exercise - Generalize RacePair

  type RaceResult[F[_], A, B] = Either[(Outcome[F, Throwable, A], Fiber[F, Throwable, B]), (Fiber[F, Throwable, A], Outcome[F, Throwable, B])]
  type EitherOutcome[F[_], A, B] = Either[Outcome[F, Throwable, A], Outcome[F, Throwable, B]]

  import cats.effect.syntax.monadCancel.*
  import cats.effect.syntax.spawn.*

  def ourRacePair[F[_], A, B](fa: F[A], fb: F[B])(using concurrent: Concurrent[F]): F[RaceResult[F, A, B]] =
    concurrent.uncancelable { poll =>
      for {
        signal <- concurrent.deferred[EitherOutcome[F, A, B]]
        fibA <- fa.guaranteeCase(outcomeA => signal.complete(Left(outcomeA)).void).start
        fibB <- fb.guaranteeCase(outcomeB => signal.complete(Right(outcomeB)).void).start
        result <- poll(signal.get).onCancel {
          // blocking call - should be cancelable
          for {
            cancelFibA <- fibA.cancel.start
            cancelFibB <- fibB.cancel.start
            _ <- cancelFibA.join
            _ <- cancelFibB.join
          } yield ()
        }
      } yield result match
        case Left(value) => Left((value, fibB))
        case Right(value) => Right((fibA, value))
  }

  override def run: IO[Unit] = polymorphicEggBoiler[IO]
}

package com.rockthejvm.part5polymorphic

import cats.effect.{Concurrent, IO, IOApp, Temporal}

import com.rockthejvm.utils.general.*

import scala.concurrent.duration.*

object PolymorphicTemporalSuspension extends IOApp.Simple {

  // Temporal - time-blocking effects
  trait MyTemporal[F[_]] extends Concurrent[F]:
    def sleep(time: FiniteDuration): F[Unit] // semantically blocks this fiber for specific time

  // abilities: pure, map/flatMap, raiseError, uncancelable, start, ref/deferred, +sleep
  val temporalIO = Temporal[IO]
  val chainOfEffects = IO("Loading...").debugM *> IO.sleep(1.second) *> IO("Game Ready!").debugM
  val chainOfEffect_v2 = temporalIO.pure("Loading...").debugM *> temporalIO.sleep(1.second) *> temporalIO.pure("Game Ready!").debugM // same

  // Exercise
  import cats.syntax.flatMap.*
  def timeout[F[_], A](fa: F[A], duration: FiniteDuration)(using temporal: Temporal[F]) : F[A] =
    val result: F[Either[A, Unit]] = temporal.race(fa, temporal.sleep(duration))

    result.flatMap {
      case Left(value) => temporal.pure(value)
      case Right(_) => temporal.raiseError(new RuntimeException("Computation timed out."))
    }

  override def run: IO[Unit] = ???
}

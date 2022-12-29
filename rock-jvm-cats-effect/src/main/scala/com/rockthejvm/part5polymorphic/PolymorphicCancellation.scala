package com.rockthejvm.part5polymorphic

import cats.effect.kernel.Outcome.{Canceled, Errored, Succeeded}
import cats.{Applicative, Monad}
import cats.effect.{IO, IOApp, MonadCancel, Poll}

import scala.concurrent.duration._

import com.rockthejvm.utils.general._

object PolymorphicCancellation extends IOApp.Simple {

  trait MyApplicativeError[F[_], E] extends Applicative[F]:
    override def pure[A](x: A): F[A] = ???
    override def ap[A, B](ff: F[A => B])(fa: F[A]): F[B] = ???

  trait MyMonadError[F[_], E] extends MyApplicativeError[F, E] with Monad[F]

  // MonadCancel
  trait MyPoll[F[_]]:
    def apply[A](fa: F[A]): F[A]

  trait MyMonadCancel[F[_], E] extends MyMonadError[F, E]:
    def cancelled: F[Unit]
    def uncancelable[A](poll: Poll[F] => F[A]): F[A]

  // monadCancel for IO
  val monadCancelIO: MonadCancel[IO, Throwable] = MonadCancel[IO]

  // we can create values
  val molIO: IO[Int] = monadCancelIO.pure(42)
  val ambitiousMolIO: IO[Int] = monadCancelIO.map(molIO)(_ * 10)

  val mustCompute = monadCancelIO.uncancelable { _ =>
    for
      _ <- monadCancelIO.pure("once started, I can't go back...")
      res <- monadCancelIO.pure(56)
    yield res
  }

  import cats.syntax.flatMap._ // flatMap
  import cats.syntax.functor._ // map

  // can generalize code
  def mustComputeGeneral[F[_], E](using mc: MonadCancel[F, E]): F[Int] = mc.uncancelable { _ =>
    for
      _ <- mc.pure("once started, I can't go back...")
      res <- mc.pure(56)
    yield res
  }

  def mustCompute_v2 = mustComputeGeneral[IO, Throwable]

  // allow cancellation listeners
  val mustComputeWithListener = mustCompute.onCancel(IO("I'm being cancelled!").void)
  val mustComputeWithListner_v2 = monadCancelIO.onCancel(mustCompute, IO("I'm being cancelled!").void) // same
  // .onCancel as extension method
  import cats.effect.syntax.monadCancel._ // .onCancel

  // allow finalizers
  val aComputationWithFinalizers = monadCancelIO.guaranteeCase(IO(42)) {
    case Succeeded(fa) => fa.flatMap(a => IO(s"successful: $a").void)
    case Errored(e) => IO(s"failed: $e").void
    case Canceled() => IO("cancelled").void
  }

  // bracket pattern is specific to MonadCancel
  val aComputationWithUsage = monadCancelIO.bracket(IO(42))(value => IO(s"Using the meaning of life: $value"))(_ => IO("releasing the meaning of life...").void)

  // Exercise

  def inputPassword[F[_], E](using mc: MonadCancel[F, E]): F[String] =
    for
      _ <- mc.pure("Input password:").debugM
      _ <- mc.pure("typing password").debugM
      _ <- unsafeSleep[F, E](5.seconds)
      pw <- mc.pure("RockTheJVM1!")
    yield pw


  def verifyPassword[F[_], E](pw: String)(using mc: MonadCancel[F, E]): F[Boolean] =
    for
      _ <- mc.pure("verifying...").debugM
      _ <- unsafeSleep[F, E](2.seconds)
      check <- mc.pure(pw == "RockTheJVM1!")
    yield check

  def authFlow[F[_], E](using mc: MonadCancel[F, E]): F[Unit] = mc.uncancelable { poll =>
    for {
      in <- poll(inputPassword).onCancel(mc.pure("Authentication Timed Out.  Try again later.").debugM.void) // this is cancelable
      verified <- verifyPassword(in) // this is NOT cancelable
      _ <- if (verified) mc.pure("Authentication successful.").debugM else mc.pure("Authentication failed.").debugM // this is NOT cancelable
    } yield ()
  }

  val authProgram: IO[Unit] = for {
    authFib <- authFlow[IO, Throwable].start
    _ <- IO.sleep(3.seconds) >> IO("Authentication timeout, attempting cancel...").debugM >> authFib.cancel
    _ <- authFib.join
  } yield ()

  override def run: IO[Unit] = authProgram
}

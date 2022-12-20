package com.rockthejvm.part3concurrency

import cats.effect.kernel.Outcome
import cats.effect.kernel.Outcome.{Canceled, Errored, Succeeded}
import cats.effect.{Fiber, FiberIO, IO, IOApp}

import concurrent.duration.{DurationInt, FiniteDuration, durationToPair}
import scala.language.postfixOps

object Fibers extends IOApp.Simple {

  val meaningOfLife = IO.pure(42)
  val favLang = IO.pure("Scala")

  import com.rockthejvm.utils._

  def sameThreadsIO() = for {
    _ <- meaningOfLife.debugM
    _ <- favLang.debugM
  } yield ()

  // introduce the Fiber
  def createFiber: Fiber[IO, Throwable, String] = ??? // almost impossible to create fibers manually

  // the fiber is not actually started, but the fiber allocation is wrapped in another effect
  val aFiber: IO[FiberIO[Int]] = meaningOfLife.debugM.start

  def differentThreadIOs() =
    for
      _ <- aFiber
      _ <- favLang.debugM
    yield ()

  // joining a fiber
  def runOnSomeOtherThread[A](io: IO[A]): IO[Outcome[IO, Throwable, A]] =
    for
      fib <- io.start
      result <- fib.join
    yield result

  /*
  * IO[ResultType of fib.join]
  * fib.join = Outcome[IO, Throwable, A]
  * possible outcomes:
  * - success with an IO
  * - failure with an exception
  * - cancelled
  * */

  val someIOOnAnotherThread = runOnSomeOtherThread(meaningOfLife)
  val someResultFromAnotherThread = someIOOnAnotherThread.flatMap {
    case Succeeded(fa) => fa
    case Errored(e) => IO(0)
    case Canceled() => IO(0)
  }

  def throwOnAnotherThread() =
    for
      fib <- IO.raiseError[Int](new RuntimeException("no number for you")).start
      result <- fib.join
    yield result

  def testCancel() =
    val task = IO("starting").debugM >> IO.sleep(1.second) >> IO("done").debugM
    val taskWithCancellationHandler = task.onCancel(IO("I'm being cancelled").debugM.void)

    for
      fib <- taskWithCancellationHandler.start // on a separate thread
      _ <- IO.sleep(500.millis) >> IO("cancelling").debugM
      _ <- fib.cancel
      result <- fib.join
    yield result

  def processResultsFromFiber[A](io: IO[A]): IO[A] = {
    val ioResult = for {
      fib <- io.debugM.start
      result <- fib.join
    } yield result

    ioResult.flatMap {
      case Succeeded(fa) => fa
      case Errored(e) => IO.raiseError(e)
      case Canceled() => IO.raiseError(new RuntimeException("Computation cancelled"))
    }
  }

  def testEx1() = {
    val aComputation = IO("starting").debugM >> IO.sleep(1.second) >> IO("done").debugM >> IO(42)
    processResultsFromFiber(aComputation).void
  }

  def tupleIOs[A, B](ioa: IO[A], iob: IO[B]): IO[(A, B)] = {
    val result = for {
      fiba <- ioa.start
      fibb <- iob.start
      resulta <- fiba.join
      resultb <- fibb.join
    } yield (resulta, resultb)

    result.flatMap {
      case (Succeeded(fa), Succeeded(fb)) => for {
          a <- fa
          b <- fb
        } yield (a, b)
      case (Errored(e), _) => IO.raiseError(e)
      case (_, Errored(e)) => IO.raiseError(e)
      case _ => IO.raiseError(new RuntimeException("Some computation cancelled"))
    }
  }

  def testEx2() = {
    val firstIO = IO.sleep(2.seconds) >> IO(1).debugM
    val secondIO = IO.sleep(3.seconds) >> IO(2).debugM
    tupleIOs(firstIO, secondIO).debugM.void
  }

  def timeout[A](io: IO[A], duration: FiniteDuration): IO[A] = {
    val computation = for {
      fib <- io.start
      _ <- (IO.sleep(duration) >> fib.cancel).start // careful - fibers can leak
      result <- fib.join
    } yield result

    computation.flatMap {
      case Succeeded(fa) => fa
      case Errored(e) => IO.raiseError(e)
      case Canceled() => IO.raiseError(new RuntimeException("Computation cancelled"))
    }
  }

  def testEx3() = {
    val aComputation = IO("starting").debugM >> IO.sleep(1.second) >> IO("done").debugM >> IO(42)
    timeout(aComputation, 500.millis).debugM.void
  }

  override def run: IO[Unit] =
    testEx3()
      .debugM
      .void
}

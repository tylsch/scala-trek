package com.rockthejvm.part3concurrency

import cats.effect.kernel.Outcome
import cats.effect.kernel.Outcome.{Canceled, Errored, Succeeded}
import cats.effect.{Fiber, FiberIO, IO, IOApp}
import concurrent.duration.DurationInt

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

  override def run: IO[Unit] =
    testCancel()
      .debugM
      .void
}

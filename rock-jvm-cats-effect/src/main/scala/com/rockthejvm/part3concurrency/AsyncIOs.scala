package com.rockthejvm.part3concurrency

import cats.effect.{IO, IOApp}

import scala.concurrent.duration.*
import com.rockthejvm.utils.*

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object AsyncIOs extends IOApp.Simple {
  // IOs can run asynchronously on fibers, without having to manually manage the fiber lifecycle
  val threadPool = Executors.newFixedThreadPool(8)
  given ec: ExecutionContext = ExecutionContext.fromExecutorService(threadPool)
  type Callback[A] = Either[Throwable, A] => Unit

  def computeMeaningOfLife(): Int = {
    Thread.sleep(1000)
    println(s"[${Thread.currentThread().getName}] computing the meaning of life on some other thread...")
    42
  }
  def computeMeaningOfLifeEither(): Either[Throwable, Int] = Try {
    computeMeaningOfLife()
  }.toEither

  def computeMolOnThreadPool(): Unit =
    threadPool.execute(() => computeMeaningOfLife())

  // lift computation to an IO
  // async is a Foreign Function Interface
  val asyncMolIO: IO[Int] = IO.async_ { cb => // Cats Effect (CE) thread blocks (semantically) until this callback is invoked (by some other thread)
    threadPool.execute { () => // computation NOT MANAGED by CE
      val result = computeMeaningOfLifeEither()
      cb(result) // CE thread is notified with the result
    }
  }

  def asyncToIO[A](computation: () => A)(ec: ExecutionContext): IO[A] =
    IO.async_[A] { (cb: Callback[A]) =>
      ec.execute { () =>
        val result = Try(computation()).toEither
        cb(result)
      }
    }

  val asyncMolIO_v2: IO[Int] = asyncToIO(computeMeaningOfLife)(ec)

  def convertFutureToIO[A](future: => Future[A]): IO[A] =
    IO.async_ { (cb: Callback[A]) =>
      future.onComplete { tryResult =>
        val result = tryResult.toEither
        cb(result)
      }
    }

  lazy val molFuture: Future[Int] = Future { computeMeaningOfLife() }
  val asyncIO_v3: IO[Int] = convertFutureToIO(molFuture)
  val asyncIO_v4: IO[Int] = IO.fromFuture(IO(molFuture))

  val neverEndingIO: IO[Int] = IO.async_[Int](_ => ()) // no callback, no finish
  val neverEndingIO_v2: IO[Int] = IO.never

  /*
  * FULLY ASYNC CALL
  * */
  def demoAsyncCancellation(): IO[Unit] = {
    val asyncMeaningOfLifeIO_v2: IO[Int] = IO.async { (cb: Callback[Int]) =>

      /*
      * finalizer in case computation gets cancelled
      * finalizers are of type IO[Unit]
      * not specifying finalizer => Option[IO[Unit]]
      * creating option iis an effect => IO[Option[IO[Unit]]]
      * */
      // return IO[Option[IO[Unit]]]
      IO {
        threadPool.execute { () =>
          val result = computeMeaningOfLifeEither()
          cb(result)
        }
      }.as(Some(IO("Cancelled!").debugM.void))
    }

    for {
      fib <- asyncMeaningOfLifeIO_v2.start
      _ <- IO.sleep(500.millis) >> IO("cancelling").debugM >> fib.cancel
      _ <- fib.join
    } yield ()
  }

  override def run: IO[Unit] = demoAsyncCancellation().debugM >> IO(threadPool.shutdown())
}

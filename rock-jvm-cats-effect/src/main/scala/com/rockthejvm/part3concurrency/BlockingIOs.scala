package com.rockthejvm.part3concurrency

import cats.effect.{IO, IOApp}

import scala.concurrent.duration.*
import com.rockthejvm.utils.*

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

object BlockingIOs extends IOApp.Simple {

  val someSleeps = for {
    _ <- IO.sleep(1.second).debugM // SEMANTIC BLOCKING
    _ <- IO.sleep(1.second).debugM
  } yield ()

  // really blocking IOs
  val aBlockingIO = IO.blocking {
    Thread.sleep(1000)
    println(s"[${Thread.currentThread().getName}] computed a blocking code")
    42
  } // will evaluate on a thread from ANOTHER thread pool specific for blocking calls

  // yielding
  val iosOnManyThreads = for {
    _ <- IO("first").debugM
    _ <- IO.cede // a signal to yield control over the thread - equivalent to IO.shift
    _ <- IO("second").debugM // the rest of this effect may run on another thread (not necessarily)
    _ <- IO.cede
    _ <- IO("third").debugM
  } yield ()

  def testThousandEffectsSwitch() = {
    val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(8))
    (1 to 1000).map(IO.pure).reduce(_.debugM >> IO.cede >> _.debugM).evalOn(ec)
  }

  /*
  * Blocking calls & IO.sleep and yield control over the calling thread automatically
  * */


  override def run: IO[Unit] = testThousandEffectsSwitch().void
}

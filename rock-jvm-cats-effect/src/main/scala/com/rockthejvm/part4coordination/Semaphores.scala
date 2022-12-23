package com.rockthejvm.part4coordination

import cats.effect.std.Semaphore
import cats.effect.{IO, IOApp}
import cats.syntax.parallel.*

import scala.concurrent.duration.*
import com.rockthejvm.utils.*

import scala.util.Random

object Semaphores extends IOApp.Simple {

  val semaphore: IO[Semaphore[IO]] = Semaphore[IO](2) // 2 total permits

  // example: limiting the number of concurrent sessions on a server
  def doWorkWhileLoggedIn(): IO[Int] = IO.sleep(1.second) >> IO(Random.nextInt(100))

  def login(id: Int, semaphore: Semaphore[IO]): IO[Int] =
    for {
      _ <- IO(s"[session $id] waiting to log in...").debugM
      _ <- semaphore.acquire
      // critical section
      _ <- IO(s"[session $id] logged in, working...").debugM
      result <- doWorkWhileLoggedIn()
      _ <- IO(s"[session $id] done: $result, logging out...").debugM
      // end of critical section
      _ <- semaphore.release
    } yield result

  def demoSemaphore() =
    for {
      sem <- Semaphore[IO](2)
      user1 <- login(1, sem).start
      user2 <- login(2, sem).start
      user3 <- login(3, sem).start
      _ <- user1.join
      _ <- user2.join
      _ <- user3.join
    } yield ()

  def weightedLogin(id: Int, requiredPermits: Int, semaphore: Semaphore[IO]): IO[Int] =
    for {
      _ <- IO(s"[session $id] waiting to log in...").debugM
      _ <- semaphore.acquireN(requiredPermits)
      // critical section
      _ <- IO(s"[session $id] logged in, working...").debugM
      result <- doWorkWhileLoggedIn()
      _ <- IO(s"[session $id] done: $result, logging out...").debugM
      // end of critical section
      _ <- semaphore.releaseN(requiredPermits)
    } yield result

  def demoWeightedSemaphore() =
    for {
      sem <- Semaphore[IO](2)
      user1 <- weightedLogin(1, 1, sem).start
      user2 <- weightedLogin(2, 2, sem).start
      user3 <- weightedLogin(3, 3, sem).start
      _ <- user1.join
      _ <- user2.join
      _ <- user3.join
    } yield ()

  val mutex = Semaphore[IO](1)
  val users: IO[List[Int]] = (1 to 10).toList.parTraverse { id =>
    for {
      sem <- mutex
      _ <- IO(s"[session $id] waiting to log in...").debugM
      _ <- sem.acquire
      // critical section
      _ <- IO(s"[session $id] logged in, working...").debugM
      result <- doWorkWhileLoggedIn()
      _ <- IO(s"[session $id] done: $result, logging out...").debugM
      // end of critical section
      _ <- sem.release
    } yield result
  }

  /*
  * 1
  * expected: all tasks start at the same time, only one can work at one time
  * reality: all tasks are parallel
  * */

  /*
  * 2
  * mistake: we flatMap Semaphore[IO](1) so we create a new semaphore every time
  * */

  // 3
  val usersFix: IO[List[Int]] = mutex.flatMap { sem =>
    (1 to 10).toList.parTraverse { id =>
      for {
        _ <- IO(s"[session $id] waiting to log in...").debugM
        _ <- sem.acquire
        // critical section
        _ <- IO(s"[session $id] logged in, working...").debugM
        result <- doWorkWhileLoggedIn()
        _ <- IO(s"[session $id] done: $result, logging out...").debugM
        // end of critical section
        _ <- sem.release
      } yield result
    }
  }

  override def run: IO[Unit] = usersFix.debugM.void
}

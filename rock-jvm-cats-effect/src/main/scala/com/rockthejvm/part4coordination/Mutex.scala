package com.rockthejvm.part4coordination

import cats.effect.{Deferred, IO, IOApp, Ref}
import cats.syntax.parallel.*

import scala.concurrent.duration.*
import com.rockthejvm.utils.*

import scala.collection.immutable.Queue
import scala.util.Random

abstract class Mutex {
  def acquire: IO[Unit]
  def release: IO[Unit]
}
object Mutex {
  type Signal = Deferred[IO, Unit]
  case class State(locked: Boolean, waiting: Queue[Signal])
  val unlocked = State(locked = false, Queue())

  def createSignal(): IO[Signal] = Deferred[IO, Unit]
  def create: IO[Mutex] = Ref[IO].of(unlocked).map { state =>
    new Mutex:
      override def acquire: IO[Unit] = createSignal().flatMap { signal =>
        state.modify {
          case State(false, _) => State(true, Queue()) -> IO.unit
          case State(true, queue) => State(true, queue.enqueue(signal)) -> signal.get
        }.flatten // modify returns IO[B], our B is IO[Unit], so modify returns IO[IO[Unit]], we need to flatten
      }

      override def release: IO[Unit] = state.modify {
        case State(false, _) => unlocked -> IO.unit
        case State(true, queue) =>
          if (queue.isEmpty) unlocked -> IO.unit
          else {
            val (signal, rest) = queue.dequeue
            State(true, rest) -> signal.complete(()).void
          }
      }.flatten
  }
}

object MutexPlayground extends IOApp.Simple {

  def criticalTask(): IO[Int] = IO.sleep(1.second) >> IO(Random.nextInt(100))
  def createNonLockingTask(id: Int): IO[Int] =
    for {
      _ <- IO(s"[task $id] working...")
      res <- criticalTask()
      _ <- IO(s"[task $id] got result: $res").debugM
    } yield res

  def demoNonLockingTasks(): IO[List[Int]] = (1 to 10).toList.parTraverse(id => createNonLockingTask(id))

  def createLockingTask(id: Int, mutex: Mutex): IO[Int] =
    for {
      _ <- IO(s"[task $id] waiting for permission...").debugM
      _ <- mutex.acquire // blocks if the mutex has been acquired by some other fiber
      // critical section
      _ <- IO(s"[task $id] working...").debugM
      res <- criticalTask()
      _ <- IO(s"[task $id] got result: $res").debugM
      // end critical section
      _ <- mutex.release
      _ <- IO(s"[task $id] lock removed").debugM
    } yield res

  def demoLockingTasks() =
    for {
      mutex <- Mutex.create
      tasks <- (1 to 10).toList.parTraverse(id => createLockingTask(id, mutex))
    } yield tasks
    // only one task will proceed at one time

  override def run: IO[Unit] = demoLockingTasks().debugM.void
}

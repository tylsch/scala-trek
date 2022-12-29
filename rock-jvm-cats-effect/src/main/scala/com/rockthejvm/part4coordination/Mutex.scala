package com.rockthejvm.part4coordination

import cats.effect.kernel.Outcome.{Canceled, Errored, Succeeded}
import cats.effect.{Deferred, IO, IOApp, Ref, Concurrent}
import cats.syntax.parallel.*

import scala.concurrent.duration.*
import com.rockthejvm.utils.*

import scala.collection.immutable.Queue
import scala.util.Random

import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.effect.syntax.monadCancel.*

abstract class MutexGen[F[_]] {
  def acquire: F[Unit]
  def release: F[Unit]
}

// generic mutex after the polymorphic concurrent exercise
object MutexGen {
  type Signal[F[_]] = Deferred[F, Unit]
  case class State[F[_]](locked: Boolean, waiting: Queue[Signal[F]])
  def unlocked[F[_]] = State[F](locked = false, Queue())

  def createSignal[F[_]](using concurrent: Concurrent[F]): F[Signal[F]] = concurrent.deferred[Unit]
  def create[F[_]](using concurrent: Concurrent[F]): F[MutexGen[F]] =
    concurrent.ref(unlocked).map(initialState => createMutexWithCancellation(initialState))

  def createMutexWithCancellation[F[_]](state: Ref[F, State[F]])(using concurrent: Concurrent[F]): MutexGen[F] = new MutexGen[F]:
    override def acquire: F[Unit] = concurrent.uncancelable { poll =>
      createSignal.flatMap { signal =>
        val cleanup = state.modify {
          case State(locked, queue) =>
            val newQueue = queue.filterNot(_ eq signal)
            State(locked, newQueue) -> release
        }.flatten

        state.modify {
          case State(false, _) => State[F](true, Queue()) -> concurrent.unit
          case State(true, queue) => State[F](true, queue.enqueue(signal)) -> poll(signal.get).onCancel(cleanup)
        }.flatten // modify returns IO[B], our B is IO[Unit], so modify returns IO[IO[Unit]], we need to flatten
      }
    }

    override def release: F[Unit] = state.modify {
      case State(false, _) => unlocked[F] -> concurrent.unit
      case State(true, queue) =>
        if (queue.isEmpty) unlocked[F] -> concurrent.unit
        else {
          val (signal, rest) = queue.dequeue
          State[F](true, rest) -> signal.complete(()).void
        }
    }.flatten
}

abstract class Mutex {
  def acquire: IO[Unit]
  def release: IO[Unit]
}

// generic mutex after the polymorphic concurrent exercise
object Mutex {
  type Signal = Deferred[IO, Unit]
  case class State(locked: Boolean, waiting: Queue[Signal])
  val unlocked = State(locked = false, Queue())

  def createSignal(): IO[Signal] = Deferred[IO, Unit]
  def create: IO[Mutex] = Ref[IO].of(unlocked).map(createMutexWithCancellation)

  def createMutexWithCancellation(state: Ref[IO, State]): Mutex = new Mutex:
    override def acquire: IO[Unit] = IO.uncancelable { poll =>
      createSignal().flatMap { signal =>
        val cleanup = state.modify {
          case State(locked, queue) =>
            val newQueue = queue.filterNot(_ eq signal)
            State(locked, newQueue) -> release
        }.flatten

        state.modify {
          case State(false, _) => State(true, Queue()) -> IO.unit
          case State(true, queue) => State(true, queue.enqueue(signal)) -> poll(signal.get).onCancel(cleanup)
        }.flatten // modify returns IO[B], our B is IO[Unit], so modify returns IO[IO[Unit]], we need to flatten
      }
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

  def createSimpleMutex(state: Ref[IO, State]): Mutex = new Mutex:
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

object MutexPlayground extends IOApp.Simple {

  def criticalTask(): IO[Int] = IO.sleep(1.second) >> IO(Random.nextInt(100))
  def createNonLockingTask(id: Int): IO[Int] =
    for {
      _ <- IO(s"[task $id] working...")
      res <- criticalTask()
      _ <- IO(s"[task $id] got result: $res").debugM
    } yield res

  def demoNonLockingTasks(): IO[List[Int]] = (1 to 10).toList.parTraverse(id => createNonLockingTask(id))

  def createLockingTask(id: Int, mutex: MutexGen[IO]): IO[Int] =
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
      mutex <- MutexGen.create[IO]
      tasks <- (1 to 10).toList.parTraverse(id => createLockingTask(id, mutex))
    } yield tasks
    // only one task will proceed at one time

  def createCancellingTask(id: Int, mutex: MutexGen[IO]): IO[Int] =
    if (id % 2 == 0) createLockingTask(id, mutex)
    else for {
      fib <- createLockingTask(id, mutex).onCancel(IO(s"[task $id] received cancellation!").debugM.void).start
      _ <- IO.sleep(2.seconds) >> fib.cancel
      out <- fib.join
      result <- out match {
        case Succeeded(effect) => effect
        case Errored(_) => IO(-1)
        case Canceled() => IO(-2)
      }
    } yield result

  def demoCancellingTasks() =
    for {
      mutex <- MutexGen.create[IO]
      tasks <- (1 to 10).toList.parTraverse(id => createLockingTask(id, mutex))
    } yield tasks

  override def run: IO[Unit] = demoCancellingTasks().debugM.void
}

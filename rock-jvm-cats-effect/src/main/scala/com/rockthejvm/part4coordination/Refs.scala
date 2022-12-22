package com.rockthejvm.part4coordination

import cats.effect.{IO, IOApp, Ref}
import cats.syntax.parallel._
import com.rockthejvm.utils._

object Refs extends IOApp.Simple {

  // ref = purely functional atomic reference
  val atomicMol: IO[Ref[IO, Int]] = Ref[IO].of(42)
  val atomicMol_v2: IO[Ref[IO, Int]] = IO.ref(42)

  // modifying is an effect
  val increaseMol: IO[Unit] = atomicMol.flatMap { ref =>
    ref.set(43) // thread-safe
  }

  // obtain a value
  val mol: IO[Int] = atomicMol.flatMap { ref =>
    ref.get // thread-safe
  }

  val gsMol: IO[Int] = atomicMol.flatMap { ref =>
    ref.getAndSet(43)
  }

  val fMol: IO[Unit] = atomicMol.flatMap { ref =>
    ref.update(value => value * 10)
  }

  val updatedMol: IO[Int] = atomicMol.flatMap { ref =>
    ref.updateAndGet(value => value * 10)
  }

  // modifying with a function returning a different type
  val modifiedMol = atomicMol.flatMap { ref =>
    ref.modify(value => (value * 10, s"my current value is $value"))
  }

  // why: concurrent + thread-safe reads/writes over shared values, in a purely functional way

  def demoConcurrentWorkImpure(): IO[Unit] = {

    var count = 0

    def task(workload: String): IO[Unit] = {
      val wordCount = workload.split(" ").length
      for {
        _ <- IO(s"Counting words for '$workload': '$wordCount''").debugM
        newCount <- IO(count + wordCount)
        _ <- IO(s"New total: $newCount").debugM
        _ <- IO(count += newCount)
      } yield ()
    }

    List("I love Cats Effect", "This ref is useless", "Daniel writes a lot of code")
      .map(task)
      .parSequence
      .void
  }
  /*
  * Drawbacks
  * - hard to read/debug
  * - mix pure/impure code
  * - NOT THREAD SAFE
  * */

  def demoConcurrentWorkPure(): IO[Unit] = {

    def task(workload: String, total: Ref[IO, Int]): IO[Unit] = {
      val wordCount = workload.split(" ").length

      for {
        _ <- IO(s"Counting words for '$workload': '$wordCount''").debugM
        newCount <- total.updateAndGet(currentCount => currentCount + wordCount)
        _ <- IO(s"New total: $newCount").debugM
      } yield ()
    }

    for {
      initCount <- Ref[IO].of(0)
      _ <- List("I love Cats Effect", "This ref thing is useless", "Daniel writes a lot of code")
        .map(string => task(string, initCount))
        .parSequence
    } yield ()
  }

  override def run: IO[Unit] = demoConcurrentWorkPure()
}

package com.rockthejvm.part4coordination

import cats.effect.std.CyclicBarrier
import cats.effect.{Deferred, IO, IOApp, Ref}
import cats.syntax.parallel.*
import com.rockthejvm.utils.*

import scala.concurrent.duration.*
import scala.util.Random

object CyclicBarriers extends IOApp.Simple {

  /*
      A cyclic barrier is a coordination primitive that
      - is initialized with a count
      - has a single API: await
      A cyclic barrier will (semantically) block all fibers calling its await() method until we have exactly N fibers waiting,
        at which point the barrier will unblock all fibers and reset to its original state.
      Any further fiber will again block until we have exactly N fibers waiting.
      ...
      And so on.
     */

  def createUser(id: Int, barrier: CBarrier): IO[Unit] =
    for
      _ <- IO.sleep((Random.nextDouble * 500).toInt.millis)
      _ <- IO(s"[user $id] just heard there's a new social network - signing up for the wait list...").debugM
      _ <- IO.sleep((Random.nextDouble * 1500).toInt.millis)
      _ <- IO(s"[user $id] on the wait list now, can't wait").debugM
      _ <- barrier.await // block the fiber when there are exactly N users waiting
      _ <- IO(s"[user $id] OMG this is so cool").debugM
    yield ()

  def openNetwork(): IO[Unit] =
    for
      _ <- IO("[announcer] the rock the jvm social network is up for registration! Launching when we have 10 users!").debugM
      barrier <- CBarrier(10)
      _ <- (1 to 20).toList.parTraverse(id => createUser(id, barrier))
    yield ()


  override def run: IO[Unit] = openNetwork()
}

abstract class CBarrier:
  def await: IO[Unit]

object CBarrier:
  case class State(nWaiting: Int, signal: Deferred[IO, Unit])
  def apply(count: Int): IO[CBarrier] =
    for
      signal <- Deferred[IO, Unit]
      state <- Ref[IO].of(State(count, signal))
    yield new CBarrier:
      override def await: IO[Unit] = Deferred[IO, Unit].flatMap { newSignal =>
        state.modify {
          case State(1, signal) => State(count, newSignal) -> signal.complete(()).void
          case State(nWaiting, signal) => State(nWaiting - 1, signal) -> signal.get
        }.flatten
      }


package com.rockthejvm.part4coordination

import cats.effect.{Deferred, FiberIO, IO, IOApp, OutcomeIO, Ref}
import cats.syntax.traverse.*
import com.rockthejvm.utils.*

import scala.concurrent.duration.*

object Defers extends IOApp.Simple {

  // deferred is a primitive for waiting for an effect, while some other effect completes with a value
  val aDeferred: IO[Deferred[IO, Int]] = Deferred[IO, Int]
  val aDeferred_v2 = IO.deferred[Int] // same

  // get = blocks the calling fiber (semantically) until some other fiber completes the Deferred with a value
  val reader: IO[Int] = aDeferred.flatMap { signal =>
    signal.get // block fiber (semantically)
  }
  val writer = aDeferred.flatMap { signal =>
    signal.complete(42)
  }

  def demoDeferred(): IO[Unit] = {
    def comsumer(signal: Deferred[IO, Int]) = for {
      _ <- IO("[consumer] waiting for result...").debugM
      meaningOfLife <- signal.get
      _ <- IO(s"[consumer] got the result: $meaningOfLife").debugM
    } yield ()

    def producer(signal: Deferred[IO, Int]) = for {
      _ <- IO("[producer] crunching numbers...").debugM
      _ <- IO.sleep(1.second)
      _ <- IO("[producer] complete: 42").debugM
      meaningOfLife <- IO(42)
      _ <- signal.complete(meaningOfLife)
    } yield ()

    for {
      signal <- Deferred[IO, Int]
      fibConsumer <- comsumer(signal).start
      fibProducer <- producer(signal).start
      _ <- fibProducer.join
      _ <- fibConsumer.join
    } yield ()
  }

  // simulate downloading some content
  val fileParts = List("I ", "love S", "cala", " with Cat", "'s Effect!<EOF>")

  def fileNotifierWithRef(): IO[Unit] = {
    def downloadFile(contentRef: Ref[IO, String]): IO[Unit] =
      fileParts
        .map { part =>
          IO(s"got '$part'").debugM >> IO.sleep(1.second) >> contentRef.update(currentContent => currentContent + part)
        }
        .sequence
        .void

    def notifyFileComplete(contentRef: Ref[IO, String]): IO[Unit] = for {
      file <- contentRef.get
      _ <- if (file.endsWith("<EOF>")) IO("File download complete").debugM else IO("downloading").debugM >> IO.sleep(500.millis) >> notifyFileComplete(contentRef)
    } yield ()

    for {
      contentRef <- Ref[IO].of("")
      fibDownloader <- downloadFile(contentRef).start
      notifier <- notifyFileComplete(contentRef).start
      _ <- fibDownloader.join
      _ <- notifier.join
    } yield ()
  }

  def fileNotifierWithDeferred(): IO[Unit] = {
    def notifyFileComplete(signal: Deferred[IO, String]): IO[Unit] =
      for {
        _ <- IO("[notifier] downloading...").debugM
        _ <- signal.get
        _ <- IO("[notifier] File download complete").debugM
      } yield ()

    def downloadFilePart(part: String, contentRef: Ref[IO, String], signal: Deferred[IO, String]): IO[Unit] =
      for {
        _ <- IO(s"[Downloader] got '$part'").debugM
        _ <- IO.sleep(1.second)
        latestContent <- contentRef.updateAndGet(currentContent => currentContent + part)
        _ <- if (latestContent.contains("<EOF>")) signal.complete(latestContent) else IO.unit
      } yield ()

    for {
      contentRef <- Ref[IO].of("")
      signal <- Deferred[IO, String]
      fibNotifier <- notifyFileComplete(signal).start
      fibFileTasks <- fileParts.map(part => downloadFilePart(part, contentRef, signal)).sequence.start
      _ <- fibNotifier.join
      _ <- fibFileTasks.join
    } yield ()
  }

  def eggBoiler(): IO[Unit] = {
    def eggReadyNotification(signal: Deferred[IO, Unit]) =
      for {
        _ <- IO("Egg boiling on some other fiber, waiting...").debugM
        _ <- signal.get
        _ <- IO("EGG READY!!!").debugM
      } yield ()

    def tickingClock(ticks: Ref[IO, Int], signal: Deferred[IO, Unit]): IO[Unit] =
      for {
        _ <- IO.sleep(500.millis)
        count <- ticks.updateAndGet(_ + 1)
        _ <- IO(count).debugM
        _ <- if (count >= 10) signal.complete(()) else tickingClock(ticks, signal)
      } yield ()

    for {
      counter <- Ref[IO].of(0)
      signal <- Deferred[IO, Unit]
      fibNotifier <- eggReadyNotification(signal).start
      clock <- tickingClock(counter, signal).start
      _ <- fibNotifier.join
      _ <- clock.join
    } yield ()
  }

  type RaceResult[A, B] = Either[(OutcomeIO[A], FiberIO[B]), (FiberIO[A], OutcomeIO[B])]
  type EitherOutcome[A, B] = Either[OutcomeIO[A], OutcomeIO[B]]
  def ourRacePair[A, B](ioa: IO[A], iob: IO[B]): IO[RaceResult[A, B]] = IO.uncancelable { poll =>
    for {
      signal <- Deferred[IO, EitherOutcome[A, B]]
      fibA <- ioa.guaranteeCase(outcomeA => signal.complete(Left(outcomeA)).void).start
      fibB <- iob.guaranteeCase(outcomeB => signal.complete(Right(outcomeB)).void).start
      result <- poll(signal.get).onCancel {
        // blocking call - should be cancelable
        for {
          cancelFibA <- fibA.cancel.start
          cancelFibB <- fibB.cancel.start
          _ <- cancelFibA.join
          _ <- cancelFibB.join
        } yield ()
      }
    } yield result match
      case Left(value) => Left((value, fibB))
      case Right(value) => Right((fibA, value))
  }

  override def run: IO[Unit] = eggBoiler()
}

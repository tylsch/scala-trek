package com.rockthejvm.part4coordination

import cats.effect.std.CountDownLatch
import cats.effect.{IO, IOApp, Resource}
import cats.syntax.traverse.*
import cats.syntax.parallel.*

import scala.concurrent.duration.*
import com.rockthejvm.utils.*

import java.io.{File, FileWriter}
import scala.io.Source
import scala.util.Random

object CountDownLatches extends IOApp.Simple {

  /*
  * CDLatches are a coordination primitive initialized with a count
  * All fibers calling await() on the CDLatch are (semantically) blocked
  * When the internal count of the latch reaches 0 (via release() calls from the other fibers), all waiting fibers are unblocked
  * */

  def announcer(latch: CountDownLatch[IO]): IO[Unit] =
    for
      _ <- IO("Starting race shortly...").debugM >> IO.sleep(2.seconds)
      _ <- IO("5...").debugM >> IO.sleep(1.second)
      _ <- latch.release
      _ <- IO("4...").debugM >> IO.sleep(1.second)
      _ <- latch.release
      _ <- IO("3...").debugM >> IO.sleep(1.second)
      _ <- latch.release
      _ <- IO("2...").debugM >> IO.sleep(1.second)
      _ <- latch.release
      _ <- IO("1...").debugM >> IO.sleep(1.second)
      _ <- latch.release
      _ <- IO("GO GO GO!").debugM
    yield ()

  def createRunner(id: Int, latch: CountDownLatch[IO]): IO[Unit] =
    for
      _ <- IO(s"[runner $id] waiting for signal...").debugM
      _ <- latch.await // block this fiber until the count reaches 0
      _ <- IO(s"[runner $id] RUNNING").debugM
    yield ()

  def sprint(): IO[Unit] =
    for
      latch <- CountDownLatch[IO](5)
      announcerFib <- announcer(latch).start
      _ <- (1 to 10).toList.parTraverse(id => createRunner(id, latch))
      _ <- announcerFib.join
    yield ()

  object FileServer {
    val fileChunksList = Array(
      "I love Scala.",
      "Cats Effect seems quite fun.",
      "Never would I have thought I would do low-level concurrency WITH pure FP."
    )

    def getNumChunks: IO[Int] = IO(fileChunksList.length)
    def getFileChunk(n: Int): IO[String] = IO(fileChunksList(n))
  }

  def writeToFile(path: String, contents: String): IO[Unit] =
    val fileResource = Resource.make(IO(new FileWriter(new File(path))))(writer => IO(writer.close()))
    fileResource.use { writer =>
      IO(writer.write(contents))
    }

  def appendFileContents(fromPath: String, toPath: String): IO[Unit] =
    val compositeResource =
      for
        reader <- Resource.make(IO(Source.fromFile(fromPath)))(source => IO(source.close()))
        writer <- Resource.make(IO(new FileWriter(new File(toPath), true)))(writer => IO(writer.close()))
      yield (reader, writer)

    compositeResource.use {
      case (reader, writer) => IO(reader.getLines().foreach(writer.write))
    }

  def createFileDownloaderTask(id: Int, latch: CountDownLatch[IO], fileName: String, destFolder: String): IO[Unit] =
    for
      _ <- IO(s"[task $id] downloading chuck...").debugM
      _ <- IO.sleep((Random.nextDouble * 1000).toInt.millis)
      chunk <- FileServer.getFileChunk(id)
      _ <- writeToFile(s"$destFolder/$fileName.part$id", chunk)
      _ <- IO(s"[task $id] chuck download complete").debugM
      _ <- latch.release
    yield ()

  def downloadFile(fileName: String, destFolder: String): IO[Unit] =
    for
      n <- FileServer.getNumChunks
      latch <- CountDownLatch[IO](n)
      _ <- IO(s"Download started on ${n} fibers.").debugM
      _ <- (0 until n).toList.parTraverse(id => createFileDownloaderTask(id, latch, fileName, destFolder))
      _ <- latch.await
      _ <- (0 until n).toList.traverse(id => appendFileContents(s"$destFolder/$fileName.part$id", s"$destFolder/$fileName"))
    yield ()


  override def run: IO[Unit] = downloadFile("myScalaFile.txt", "src/main/resources")
}

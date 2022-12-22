package com.rockthejvm.part3concurrency

import cats.effect.{IO, IOApp}

import java.io.{File, FileReader}
import java.util.Scanner
import scala.concurrent.duration.*

object Resources extends IOApp.Simple {
  import com.rockthejvm.utils._

  // use-case: manage a connection lifecycle
  class Connection(url: String) {
    def open: IO[String] = IO(s"opening connection to $url").debugM
    def close: IO[String] = IO(s"closing connection to $url").debugM
  }

  val asyncFetchUrl = for {
    fib <- (new Connection("rockthejv.com").open *> IO.sleep((Int.MaxValue).seconds)).start
    _ <- IO.sleep(1.second) *> fib.cancel
  } yield ()
  // problem: leaking resources

  val correctAsyncFetchUrl = for {
    conn <- IO(new Connection("rockthejv.com"))
    fib <- (conn.open *> IO.sleep((Int.MaxValue).seconds)).onCancel(conn.close.void).start
    _ <- IO.sleep(1.second) *> fib.cancel
  } yield ()

/*
* bracket pattern: someIO.bracket(useResourceCallBack)(releaseResourceCallback)
* bracket is equivalent to try-catches (pure FP)
* */
  val bracketFetchUrl = IO(new Connection("rockthejvm.com"))
    .bracket(conn => conn.open *> IO.sleep(Int.MaxValue.seconds))(conn => conn.close.void)

  val bracketProgram = for {
    fib <- bracketFetchUrl.start
    _ <- IO.sleep(1.second) *> fib.cancel
  } yield ()

  def openFileScanner(path: String): IO[Scanner] =
    IO(new Scanner(new FileReader(new File(path))))

  def readLineByLine(scanner: Scanner): IO[Unit] =
    if (scanner.hasNextLine) IO(scanner.nextLine()).debugM >> IO.sleep(100.millis) >> readLineByLine(scanner)
    else IO.unit

  def closeScanner(scanner: Scanner, path: String): IO[Unit] = IO(s"closing file at $path").debugM >> IO(scanner.close())

  def bracketReadFile(path: String): IO[Unit] =
    IO(s"opening file at $path") >>
      openFileScanner(path).bracket(readLineByLine)(scanner => closeScanner(scanner, path))

  override def run: IO[Unit] = bracketReadFile("src/main/scala/com/rockthejvm/part3concurrency/Resources.scala")
}

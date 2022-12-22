package com.rockthejvm.part3concurrency

import cats.effect.kernel.Outcome.{Canceled, Errored, Succeeded}
import cats.effect.{IO, IOApp, Resource}

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

  /*
  * Resources
  * */
  def connFromConfig(path: String): IO[Unit] =
    openFileScanner(path)
      .bracket { scanner =>
        IO(new Connection(scanner.nextLine()))
          .bracket(conn => conn.open.debugM >> IO.never)(conn => conn.close.debugM.void)
      }(scanner => IO("closing file").debugM >> IO(scanner.close()))

  // nesting resources are tedious
  val connectionResource = Resource.make(IO(new Connection("rockthejvm.com")))(conn => conn.close.void)
  // ... at a later part of your code
  val resourceFetchUrl = for {
    fib <- connectionResource.use(conn => conn.open >> IO.never).start
    _ <- IO.sleep(1.second) >> fib.cancel
  } yield ()

  // resources are equivalent to brackets
  val simpleResources = IO("some resource")
  val usingResource: String => IO[String] = string => IO(s"using the string: $string").debugM
  val releaseResource: String => IO[Unit] = string => IO(s"finalizing the string: $string").debugM.void

  val usingResourceWithBracket = simpleResources.bracket(usingResource)(releaseResource)
  val usingResourceWithResource = Resource.make(simpleResources)(releaseResource).use(usingResource)

  def getResourceFromFile(path: String) = Resource.make(openFileScanner(path)) { scanner =>
    IO(s"closing file at $path").debugM >> IO(scanner.close())
  }

  def resourceReadFile(path: String) = IO(s"opening file at $path") >>
    getResourceFromFile(path).use { scanner =>
      if (scanner.hasNextLine) IO(scanner.nextLine()).debugM >> IO.sleep(100.millis) >> readLineByLine(scanner)
      else IO.unit
  }

  def cancelReadFile(path: String) = for {
    fib <- resourceReadFile(path).start
    _ <- IO.sleep(2.seconds) >> fib.cancel
  } yield ()

  // nested resources
  def connFromConfResource(path: String) =
    Resource.make(IO("opening fle").debugM >> openFileScanner(path))(scanner => IO("closing file").debugM >> IO(scanner.close()))
      .flatMap(scanner => Resource.make(IO(new Connection(scanner.nextLine())))(conn => conn.close.void))

  def connFromConfResourceClean(path: String) = for {
    scanner <- Resource.make(IO("opening fle").debugM >> openFileScanner(path))(scanner => IO("closing file").debugM >> IO(scanner.close()))
    conn <- Resource.make(IO(new Connection(scanner.nextLine())))(conn => conn.close.void)
  } yield conn

  val openConnection = connFromConfResourceClean("src/main/resources/connection.txt").use(conn => conn.open >> IO.never)
  val canceledConnection = for {
    fib <- openConnection.start
    _ <- IO.sleep(1.second) >> IO("cancelling").debugM >> fib.cancel
  } yield ()
  // connection + file will close automatically

  // finalizers to regular IOs
  val ioWithFinalizer = IO("some resource").debugM.guarantee(IO("freeing resource").debugM.void)
  val ioWthFinalizer_v2 = IO("some resource").debugM.guaranteeCase {
    case Succeeded(fa) => fa.flatMap(result => IO(s"releasing resource: $result").debugM).void
    case Errored(e) => IO("nothing to release").debugM.void
    case Canceled() => IO("resource got cancelled, releasing what's left").debugM.void
  }

  override def run: IO[Unit] = ioWithFinalizer.void
}

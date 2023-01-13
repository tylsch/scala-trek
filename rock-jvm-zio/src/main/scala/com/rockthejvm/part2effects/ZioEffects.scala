package com.rockthejvm.part2effects

import zio.*

import scala.io.StdIn

object ZioEffects {

  val meaningOfLife: ZIO[Any, Nothing, Int] = ZIO.succeed(42)
  val aFailure: ZIO[Any, String, Nothing] = ZIO.fail("Something went wrong")
  val aSuspendedZio: RIO[Any, RuntimeFlags] = ZIO.suspend(meaningOfLife)

  val improveMOL = meaningOfLife.map(_ * 2)
  val printingMOL = meaningOfLife.flatMap(mol => ZIO.succeed(println(mol)))

  val smallProgram =
    for
      _ <- ZIO.succeed(println("What's your name"))
      name <- ZIO.succeed(StdIn.readLine())
      _ <- ZIO.succeed(println(s"Welcome to ZIO, $name"))
    yield ()

  // A LOT of combinators
  // zip
  val anotherMOL = ZIO.succeed(100)
  val tupledZIO = meaningOfLife.zip(anotherMOL)
  val combinedZIO = meaningOfLife.zipWith(anotherMOL)(_ * _)

  /*
  * Type aliases of ZIOs
  * */
  // UIO[A] = ZIO[Any, Nothing, A] - no requirements, cannot fail, produces A
  val aUIO: UIO[Int] = ZIO.succeed(99)
  // URIO[R,A] = ZIO[R,Nothing,A] - cannot fail
  val aURIO: URIO[Int, Int] = ZIO.succeed(67)
  // RIO[R,A] = ZIO[R,Throwable, A] - can fail with a Throwable
  val anRIO: RIO[Int, Int] = ZIO.succeed(98)
  val aFailedRIO: RIO[Int, Int] = ZIO.fail(new RuntimeException("RIO failed"))
  // Task[A] = ZIO[Any, Throwable, A] - no requirements, can fail with a Throwable, produces A
  val aSuccessfulTask: Task[Int] = ZIO.succeed(89)
  val aFailedTask: Task[Int] = ZIO.fail(new RuntimeException("Something bad"))
  // IO[E,A] = ZIO[Any,E,A] - no requirements
  val aSuccessfulIO: IO[String, Int] = ZIO.succeed(34)
  val aFailedIO: IO[String, Int] = ZIO.fail("Something bad happened")

  // 1 - sequence two ZIOs and take the values of the last one
  def sequenceTakeLast[R, E, A, B](zioa: ZIO[R, E, A], ziob: ZIO[R, E, B]): ZIO[R, E, B] =
    zioa.flatMap(_ => ziob.map(b => b))

  def sequenceTakeLast_v2[R, E, A, B](zioa: ZIO[R, E, A], ziob: ZIO[R, E, B]): ZIO[R, E, B] =
    for
      _ <- zioa
      b <- ziob
    yield b

  def sequenceTakeLast_v3[R, E, A, B](zioa: ZIO[R, E, A], ziob: ZIO[R, E, B]): ZIO[R, E, B] =
    zioa *> ziob

  // 2 - sequence two ZIOs and take the values of the first one
  def sequenceTakeFirst[R, E, A, B](zioa: ZIO[R, E, A], ziob: ZIO[R, E, B]): ZIO[R, E, A] =
    for
      a <- zioa
      _ <- ziob
    yield a

  def sequenceTakeFirst_v2[R, E, A, B](zioa: ZIO[R, E, A], ziob: ZIO[R, E, B]): ZIO[R, E, A] =
    zioa <* ziob

  // 3 - run a ZIO forever
  def runForever[R, E, A](zio: ZIO[R, E, A]): ZIO[R, E, A] =
    zio.flatMap(_ => runForever(zio))

  def runForever_v2[R, E, A](zio: ZIO[R, E, A]): ZIO[R, E, A] =
    zio *> runForever_v2(zio)

  val endlessLoop = runForever {
    ZIO.succeed {
      println("running...")
      Thread.sleep(1000)
    }
  }

  // 4 - convert the value of a ZIO to something else
  def convert[R, E, A, B](zio: ZIO[R, E, A], value: B): ZIO[R, E, B] =
    zio.map(_ => value)

  def convert_v2[R, E, A, B](zio: ZIO[R, E, A], value: B): ZIO[R, E, B] =
    zio.as(value)

  // 5 - discard the value of ZIO to a Unit
  def asUnit[R, E, A](zio: ZIO[R, E, A]): ZIO[R, E, Unit] =
    convert(zio, ())

  def asUnit_v2[R, E, A](zio: ZIO[R, E, A]): ZIO[R, E, Unit] =
    zio.unit

  // 6 - recursion
  def sum(n: Int): Int =
    if (n == 0) 0
    else n + sum(n - 1) // will crash at sum(20000)

  def sumZIO(n: Int): UIO[Int] =
    if (n == 0) ZIO.succeed(0)
    else
      for
        current <- ZIO.succeed(n)
        prevSum <- sumZIO(n - 1)
      yield current + prevSum

  // 7 - fibonacci
  def fibo(n: Int): BigInt =
    if (n <= 2) 1
    else fibo(n - 1) + fibo(n - 2)

  def fiboZIO(n: Int): UIO[BigInt] =
    if (n <= 2) ZIO.succeed(1)
    else
      for
        last <- ZIO.suspendSucceed(fiboZIO(n - 1))
        prev <- fiboZIO(n - 2)
      yield last + prev


  def main(args: Array[String]): Unit = {
    val runtime = Runtime.default
    implicit val trace: Trace = Trace.empty
    Unsafe.unsafe { implicit u =>
      val firstEffect = ZIO.succeed {
        println("Computing first effect")
        Thread.sleep(1000)
        1
      }
      val secondEffect = ZIO.succeed {
        println("Computing second effect")
        Thread.sleep(1000)
        2
      }

      println(runtime.unsafe.run(fiboZIO(10)))
    }
  }
}

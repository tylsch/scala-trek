package com.rockthejvm.part2effects

import cats.effect.IO

import scala.annotation.tailrec
import scala.io.StdIn

object IOIntroduction {

  // IO
  val ourFirstIO: IO[Int] = IO.pure(42)
  val aDelayedIO: IO[Int] = IO.delay({
    println("I'm producing an integer")
    54
  })

  val aDelayedIO_v2: IO[Int] = IO { // apply == delay
    println("I'm producing an integer")
    54
  }

  // map, flatMap
  val improvedMeaningOfLife = ourFirstIO.map(_ * 2)
  val printedMeaningOfLife = ourFirstIO.flatMap(mol => IO.delay(println(mol)))

  def smallProgram(): IO[Unit] = for {
    line1 <- IO(StdIn.readLine())
    line2 <- IO(StdIn.readLine())
    _ <- IO.delay(println(line1 + line2))
  } yield ()

  // mapN - combine IO effects as tuples
  import cats.syntax.apply._
  val combinedMeaningOfLife: IO[Int] = (ourFirstIO, improvedMeaningOfLife).mapN(_ + _)
  def smallProgram_v2(): IO[Unit] =
    (IO(StdIn.readLine()), IO(StdIn.readLine())).mapN(_ + _).map(println)

  def sequenceTakeLast[A, B](ioa: IO[A], iob: IO[B]): IO[B] =
    ioa.flatMap(_ => iob)

  def sequenceTakeLast2[A, B](ioa: IO[A], iob: IO[B]): IO[B] =
    ioa *> iob // andThen

  def sequenceTakeLast3[A, B](ioa: IO[A], iob: IO[B]): IO[B] =
    ioa >> iob // andThen wth by-name call

  def sequenceTakeFirst[A, B](ioa: IO[A], iob: IO[B]): IO[A] =
    ioa.flatMap(a => iob.map(_ => a))

  def sequenceTakeFirst2[A, B](ioa: IO[A], iob: IO[B]): IO[A] =
    ioa <* iob // "<<" lazy version of "<*"


  def forever[A](io: IO[A]): IO[A] =
    io.flatMap(_ => forever(io))

  def forever2[A](io: IO[A]): IO[A] =
    io >> forever2(io)

  def forever3[A](io: IO[A]): IO[A] =
    io *> forever3(io) // StackOverflowError due to eager evaluation

  def forever4[A](io: IO[A]): IO[A] =
    io.foreverM // with tail recursion

  def convert[A, B](ioa: IO[A], value: B): IO[B] =
    ioa.map(_ => value)

  def convert2[A, B](ioa: IO[A], value: B): IO[B] =
    ioa.as(value) // same

  def asUnit[A](ioa: IO[A]): IO[Unit] =
    ioa.map(_ => ())

  def asUnit2[A](ioa: IO[A]): IO[Unit] =
    ioa.as(()) // discourage = don't use

  def asUnit3[A](ioa: IO[A]): IO[Unit] =
    ioa.void // same - encourage

  def sum(n: Int): Int =
    if (n <= 0) 0
    else n + sum(n - 1)

  def sumIO(n: Int): IO[Int] =
    if (n <= 0) IO(0)
    else for {
      lastNumber <- IO(n)
      prevSum <- sumIO(n - 1)
    } yield prevSum + lastNumber

  def fibonacci(n: Int): IO[BigInt] =
    if (n < 2) IO(1)
    else for {
      last <- IO.defer(fibonacci(n - 1)) // same as .delay(...).flatten
      prev <- IO.defer(fibonacci(n - 2))
    } yield last + prev


  def main(args: Array[String]): Unit = {
    import cats.effect.unsafe.implicits.global // "platform"
    // "end of the world"
    //println(smallProgram_v2().unsafeRunSync())
    //println(sumIO(20000).unsafeRunSync())
    (1 to 100).foreach(i => println(fibonacci(i).unsafeRunSync()))
    //sum(20000)
  }
}

package com.rockthejvm.part2effects

import cats.Traverse
import cats.effect.{IO, IOApp}

import scala.concurrent.Future
import scala.util.Random

object IOTraversal extends IOApp.Simple {

  import scala.concurrent.ExecutionContext.Implicits.global

  def heavyComputation(string: String): Future[Int] = Future {
    Thread.sleep(Random.nextInt(1000))
    string.split(" ").length
  }

  def workLoad: List[String] = List("I quite like CE", "Scala is great", "looking forward to some awesome stuff")

  def clunkyFutures(): Unit = {
    val futures: List[Future[Int]] = workLoad.map(heavyComputation)
    // Future[List[Int]] would be hard to obtain
    futures.foreach(_.foreach(println))
  }

  import cats.instances.list._
  val listTraverse = Traverse[List]
  def traverseFutures(): Unit = {
    // traverse
    val singleFuture: Future[List[Int]] = listTraverse.traverse(workLoad)(heavyComputation)
    // ^^ stores all the results
    singleFuture.foreach(println)
  }

  // traverse for IO
  import com.rockthejvm.utils._
  def computeAsIO(string: String): IO[Int] = IO {
    Thread.sleep(Random.nextInt(1000))
    string.split(" ").length
  }.debugM

  val ios: List[IO[Int]] = workLoad.map(computeAsIO)
  val singleIO: IO[List[Int]] = listTraverse.traverse(workLoad)(computeAsIO)

  // parallel traversal
  import cats.syntax.parallel._
  val parallelSingleIO: IO[List[Int]] = workLoad.parTraverse(computeAsIO)

  def sequence[A](listOfIOs: List[IO[A]]): IO[List[A]] =
    listTraverse.traverse(listOfIOs)(identity) // lambda(elem) = IO[A]

  def sequence_v2[F[_] : Traverse, A](listOfIOs: F[IO[A]]): IO[F[A]] =
    Traverse[F].traverse(listOfIOs)(identity)

  def parSequence[A](listOfIOs: List[IO[A]]): IO[List[A]] =
    listOfIOs.parTraverse(identity)

  def parSequence_v2[F[_] : Traverse, A](listOfIOs: F[IO[A]]): IO[F[A]] =
    listOfIOs.parTraverse(identity)

  // existing sequence API
  val singleIO_v2: IO[List[Int]] = listTraverse.sequence(ios)

  // parallel sequence
  val parallelSingleIO_v2: IO[List[Int]] = parSequence(ios)
  val parallelSingleIO_v3: IO[List[Int]] = ios.parSequence // extension method from the Parallel syntax package


  override def run: IO[Unit] = parallelSingleIO.map(_.sum).debugM.void
}

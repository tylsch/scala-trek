package lectures.part5ts

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object HigherKindedTypes extends App {
  trait AHigherKindedType[F[_]]
  trait MyList[T] {
    def flatMap[B](f: T => B): MyList[B]
  }

  trait MyOption[T] {
    def flatMap[B](f: T => B): MyOption[B]
  }

  trait MyFuture[T] {
    def flatMap[B](f: T => B): MyFuture[B]
  }

  // combine/multiply List(1,2) x List("a","b")

//  def multiply[A, B](listA: List[A], listB: List[B]): List[(A, B)] =
//    for {
//      a <- listA
//      b <- listB
//    } yield (a, b)
//
//  def multiply[A, B](listA: Option[A], listB: Option[B]): Option[(A, B)] =
//    for {
//      a <- listA
//      b <- listB
//    } yield (a, b)
//
//  def multiply[A, B](listA: Future[A], listB: Future[B]): Future[(A, B)] =
//    for {
//      a <- listA
//      b <- listB
//    } yield (a, b)

  trait Monad[F[_], A] { // Higher-Kinded Type Class
    def flatMap[B](f: A => F[B]): F[B]
    def map[B](f: A => B): F[B]
  }

  implicit class MonadList[A](list: List[A]) extends Monad[List, A] {
    override def flatMap[B](f: A => List[B]): List[B] = list.flatMap(f)
    override def map[B](f: A => B): List[B] = list.map(f)
  }

  implicit class MonadOption[A](list: Option[A]) extends Monad[Option, A] {
    override def flatMap[B](f: A => Option[B]): Option[B] = list.flatMap(f)
    override def map[B](f: A => B): Option[B] = list.map(f)
  }

  def multiply[F[_], A, B](implicit ma: Monad[F, A], mb: Monad[F, B]): F[(A, B)] =
    for {
      a <- ma
      b <- mb
    } yield (a, b)

  val monadList = new MonadList(List(1,2,3))
  val newList = monadList.flatMap(x => List(x, x + 1))
  val doubleMonadList = monadList.map(_ * 2)

  println(multiply(List(1,2), List("a","b")))
  println(multiply(Some(2), Some("Scala")))
}

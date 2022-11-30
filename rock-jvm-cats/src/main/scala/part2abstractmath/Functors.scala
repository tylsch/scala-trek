package part2abstractmath

import scala.util.Try

object Functors {

  val aModifiedList = List(1,2,3).map(_ + 1)
  val aModifiedOption = Option(2).map(_ + 1)
  val aModifiedTry = Try(42).map(_ + 1)

  // simplified definition
  trait MyFunctor[F[_]] {
    def map[A, B](initialValue: F[A])(f: A => B): F[B]
  }

  import cats.Functor
  import cats.instances.list._
  val listFunctor = Functor[List]
  val incrementedNumbers = listFunctor.map(List(1,2,3))(_ + 1)

  import cats.instances.option._
  val optionFunctor = Functor[Option]
  val incrementedOption = optionFunctor.map(Option(2))(_ + 1)

  import cats.instances.try_._
  val anIncrementedTry = Functor[Try].map(Try(42))(_ + 1)

  // generalize an API
  def do10xList(list: List[Int]): List[Int] = list.map(_ * 10)
  def do10xOption(option: Option[Int]): Option[Int] = option.map(_ * 10)
  def do10xTry(attempt: Try[Int]): Try[Int] = attempt.map(_ * 10)

  def do10x[F[_]](container: F[Int])(implicit functor: Functor[F]): F[Int] = functor.map(container)(_ * 10)

  trait Tree[+T]
  object Tree {
    def leaf[T](value: T): Leaf[T] = Leaf(value)
    def branch[T](value: T, left: Tree[T], right: Tree[T]): Branch[T] = Branch(value, left, right)
  }
  case class Leaf[+T](value: T) extends Tree[T]
  case class Branch[+T](value: T, left: Tree[T], right: Tree[T]) extends Tree[T]
  implicit object TreeFunctor extends Functor[Tree] {
    override def map[A, B](fa: Tree[A])(f: A => B): Tree[B] = fa match {
      case Leaf(value) => Leaf(f(value))
      case Branch(value, left, right) => Branch(f(value), map(left)(f), map(right)(f))
    }
  }

  // extension method - map
  import cats.syntax.functor._
  val tree: Tree[Int] = Branch(40, Branch(5, Leaf(10), Leaf(30)), Leaf(20))
  val incrementedTree = tree.map(_ + 1)

  def do10xShorter[F[_] : Functor](container: F[Int]): F[Int] = container.map(_ * 10)

  def main(args: Array[String]): Unit = {
    println(do10x(List(1,2,3)))
    println(do10x(Option(2)))
    println(do10x(Try(45)))
    println(do10x[Tree](Branch(30, Leaf(10), Leaf(20))))
    println(do10xShorter[Tree](Branch(30, Leaf(10), Leaf(20))))
  }
}

package part4typeclasses

import cats.{Applicative, Monad}

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}

object Traversing {

  implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(8))
  val servers = List("server-ci.rockthejvm.com", "server-staging.rockthejvm.com", "prod.rockthejvm.com")
  def getBandwidth(hostname: String): Future[Int] = Future(hostname.length * 80)

  val allBandwidths: Future[List[Int]] = servers.foldLeft(Future(List.empty[Int])) { (accumulator, hostname) =>
    val bandFuture: Future[Int] = getBandwidth(hostname)
    for {
      accBandwidths <- accumulator
      band <- bandFuture
    } yield accBandwidths :+ band
  }

  val allBandwidthsTraverse: Future[List[Int]] = Future.traverse(servers)(getBandwidth)
  val allBandwidthsSequence: Future[List[Int]] = Future.sequence(servers.map(getBandwidth))

  import cats.syntax.applicative._
  import cats.syntax.flatMap._
  import cats.syntax.functor._
  import cats.syntax.apply._
  def listTraverse[F[_] : Applicative, A, B](list: List[A])(func: A => F[B]): F[List[B]] =
    list.foldLeft(List.empty[B].pure[F]) { (accumulator, element) =>
      val wElement: F[B] = func(element)
      (accumulator, wElement).mapN(_ :+ _)
    }

  def listSequence[F[_] : Applicative, A](list: List[F[A]]): F[List[A]] =
    listTraverse(list)(identity)

  import cats.instances.vector._
  val p1 = listSequence(List(Vector(1,2), Vector(3,4))) // Vector[List[Int]] - all possible 2-tuples
  val p2 = listSequence(List(Vector(1,2), Vector(3,4), Vector(5, 6))) // Vector[List[Int]] - all possible 3-tuples

  def main(args: Array[String]): Unit = {
    println(p1)
    println(p2)
  }
}

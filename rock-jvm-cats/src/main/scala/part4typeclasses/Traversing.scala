package part4typeclasses

import cats.{Applicative, Foldable, Functor, Monad}

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

  import cats.instances.option._
  def filterAsOption(list: List[Int])(predicate: Int => Boolean): Option[List[Int]] =
    listTraverse[Option, Int, Int](list)(n => Some(n).filter(predicate))

  val p3: Option[List[Int]] = filterAsOption(List(2,4,6))(_ % 2 == 0)
  val p4: Option[List[Int]] = filterAsOption(List(1,2,3))(_ % 2 == 0)

  import cats.data.Validated
  type ErrorsOr[T] = Validated[List[String], T]
  def filterAsValidated(list: List[Int])(predicate: Int => Boolean): ErrorsOr[List[Int]] =
    listTraverse[ErrorsOr, Int, Int](list) { n =>
      if (predicate(n)) Validated.valid(n)
      else Validated.invalid(List(s"predicate for $n failed"))
    }

  val p5: ErrorsOr[List[Int]] = filterAsValidated(List(2, 4, 6))(_ % 2 == 0)
  val p6: ErrorsOr[List[Int]] = filterAsValidated(List(1, 2, 3))(_ % 2 == 0)

  trait MyTraverse[L[_]] extends Foldable[L] with Functor[L] {
    def traverse[F[_] : Applicative, A, B](container: L[A])(func: A => F[B]): F[L[B]]
    def sequence[F[_] : Applicative, A](container: L[F[A]]): F[L[A]] = traverse(container)(identity)

    import cats.Id
    def map[A, B](wa: L[A])(f: A => B): L[B] =
      traverse[Id, A, B](wa)(f)
  }

  import cats.Traverse
  val allBandwidthsCats = Traverse[List].traverse(servers)(getBandwidth)

  // extension methods
  import cats.syntax.traverse._
  val allBandwidthsCats2 = servers.traverse(getBandwidth)


  def main(args: Array[String]): Unit = {
    println(p1)
    println(p2)
    println(p3)
    println(p4)
    println(p5)
    println(p6)
  }
}

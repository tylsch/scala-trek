package part2abstractmath

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}

object Monads {

  // lists
  val numbersList = List(1,2,3)
  val charsList = List('a' ,'b', 'c')
  val combinationsList: List[(Int, Char)] = numbersList.flatMap(n => charsList.map(c => (n, c)))
  val combinationsListFor = for {
    n <- numbersList
    c <- charsList
  } yield (n, c) // identical

  // options
  val numberOption = Option(2)
  val charOption = Option('d')
  val combinationOption = numberOption.flatMap(n => charOption.map(c => (n, c)))
  val combinationOptionFor = for {
    n <- numberOption
    c <- charOption
  } yield (n, c)

  // futures
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(8))
  val numberFuture = Future(42)
  val charFuture = Future('z')
  val combinationFuture = numberFuture.flatMap(n => charFuture.map(c => (n, c)))
  val combinationFutureFor = for {
    n <- numberFuture
    c <- charFuture
  } yield (n, c)

  /*
  * Pattern
  * - wrapping a value into a monadic value
  * - the flatMap mechanism
  *
  * MONADS
  * */
  trait MyMonad[M[_]] {
    def pure[A](value: A): M[A]
    def flatMap[A, B](ma: M[A])(f: A => M[B]): M[B]
  }

  // Cats monad
  import cats.Monad
  import cats.instances.option._

  val optionMonad = Monad[Option]
  val anOption = optionMonad.pure(4) // Option(4) == Some(4)
  val aTransformedOption = optionMonad.flatMap(anOption)(x => if (x % 3 == 0) Some(x + 1) else None)

  import cats.instances.list._
  val listMonad = Monad[List]
  val aList = listMonad.pure(3) // List(3)
  val aTransformedList = listMonad.flatMap(aList)(x => List(x, x + 1)) // List(4, 5)

  import cats.instances.future._
  val futureMonad = Monad[Future]
  val aFuture = futureMonad.pure(42) // requires an implicit ExecutionContext
  val aTransformedFuture = futureMonad.flatMap(aFuture)(x => Future(x + 44))

  // specialized API
  def getPairsList(numbers: List[Int], chars: List[Char]): List[(Int, Char)] = numbers.flatMap(n => chars.map(c => (n, c)))
  def getPairsOption(number: Option[Int], char: Option[Char]): Option[(Int, Char)] = number.flatMap(n => char.map(c => (n, c)))
  def getPairsFuture(number: Future[Int], char: Future[Char]): Future[(Int, Char)] = number.flatMap(n => char.map(c => (n, c)))

  // generalize
  def getPairs[M[_], A, B](ma: M[A], mb: M[B])(implicit monad: Monad[M]): M[(A, B)] =
    monad.flatMap(ma)(a => monad.map(mb)(b => (a, b)))

  def main(args: Array[String]): Unit = {
    println(getPairs(numbersList, charsList))
    println(getPairs(numberOption, charOption))
    getPairs(numberFuture, charFuture).foreach(println)
  }
}

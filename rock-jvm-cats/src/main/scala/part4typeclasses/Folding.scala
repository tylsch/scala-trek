package part4typeclasses

import cats.{Eval, Monoid}

object Folding {

  object ListExercises {
    def map[A, B](list: List[A])(f: A => B): List[B] =
      list.foldRight(List.empty[B])((a, currentList) => f(a) :: currentList)

    def flatMap[A, B](list: List[A])(f: A => List[B]): List[B] =
      list.foldLeft(List.empty[B])((currentList, a) => currentList.foldRight(f(a))(_ :: _))

    def filter[A](list: List[A])(predicate: A => Boolean): List[A] =
      list.foldRight(List.empty[A])((a, currentList) => if (predicate(a)) a :: currentList else currentList)
    def combineAll[A](list: List[A])(implicit monoid: Monoid[A]): A =
      list.foldLeft(monoid.empty)(monoid.combine)
  }

  import cats.Foldable
  import cats.instances.list._
  val sum = Foldable[List].foldLeft(List(1,2,3), 0)(_ + _) // 6
  import cats.instances.option._
  val sumOption = Foldable[Option].foldLeft(Option(2), 30)(_ + _) // 32

  // foldRight is stack-safe regardless of your container
  val sumRight: Eval[Int] = Foldable[List].foldRight(List(1,2,3), Eval.now(0)) { (num, eval) =>
    eval.map(_ + num)
  }

  import cats.instances.int._
  import cats.instances.string._
  val anotherSum = Foldable[List].combineAll(List(1,2,3))
  val mappedConcat = Foldable[List].foldMap(List(1,2,3))(_.toString) // => "123"

  import cats.instances.vector._
  val intsNested = List(Vector(1,2,3), Vector(4,5,6))
  (Foldable[List] compose Foldable[Vector]).combineAll(intsNested)

  // extension methods
  import cats.syntax.foldable._
  val sum3 = List(1,2,3).combineAll
  val mappedConcat2 = List(1,2,3).foldMap(_.toString)


  def main(args: Array[String]): Unit = {
    import ListExercises._
    val numbers = (1 to 10).toList
    println(map(numbers)(_ + 1))
    println(flatMap(numbers)(x => (1 to x).toList))
    println(filter(numbers)(_ % 2 == 0))

    println(combineAll(numbers))
  }
}

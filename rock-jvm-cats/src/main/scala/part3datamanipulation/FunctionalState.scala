package part3datamanipulation

import cats.Eval
import cats.data.IndexedStateT

object FunctionalState {

  type MyState[S, A] = S => (S, A)

  import cats.data.State
  val countAndSay: State[Int, String] = State(currentCount => (currentCount + 1, s"Counted $currentCount"))
  val (eleven, counted10) = countAndSay.run(10).value
  // state = "iterative" computations

  var a = 10
  a += 1
  val firstComputation = s"Added 1 to 10, obtained ${a}"
  a *= 5
  val secondComputation = s"Multiplied with 5, obtained ${a}"

  // pure FP with states
  val firstTransformation = State((s: Int) => (s + 1, s"Added 1 to 10, obtained ${s + 1}"))
  val secondTransformation = State((s: Int) => (s * 5, s"Multiplied with 5, obtained ${s * 5}"))
  val compositeTransformation: State[Int, (String, String)] = firstTransformation.flatMap { first =>
    secondTransformation.map(second => (first, second))
  }
  val compositeTransformation2 = for {
    first <- firstTransformation
    second <- secondTransformation
  } yield (first, second)

  val func1 = (s: Int) => (s + 1, s"Added 1 to 10, obtained ${s + 1}")
  val func2 = (s: Int) => (s * 5, s"Multiplied with 5, obtained ${s * 5}")
  val compositeFunc = func1.andThen {
    case (newState, first) => (first, func2(newState))
  }

  case class ShoppingCart(items: List[String], total: Double)
  def addToCart(item: String, price: Double): State[ShoppingCart, Double] = State { cart =>
    (ShoppingCart(item :: cart.items, cart.total + price), price + cart.total)
  }

  val danielsCart: State[ShoppingCart, Double] = for {
    _ <- addToCart("Fender guitar", 500)
    _ <- addToCart("Elixir strings", 19)
    total <- addToCart("Electric cable", 8)
  } yield total

  def inspect[A, B](f: A => B): State[A, B] = State((a: A) => (a, f(a)))
  def get[A]: State[A, A] = State((a: A) => (a, a))
  def set[A](value: A): State[A, Unit] = State((_: A) => (value, ()))
  def modify[A](f: A => A): State[A, Unit] = State((a: A) => (f(a), ()))

  // methods available
  import cats.data.State._

  val program: State[Int, (Int, Int, Int)] = for {
    a <- get[Int]
    _ <- set[Int](a + 10)
    b <- get[Int]
    _ <- modify[Int](_ + 43)
    c <- inspect[Int, Int](_ * 2)
  } yield (a, b, c)


  def main(args: Array[String]): Unit = {
    println(compositeTransformation2.run(10).value)
    println(compositeFunc(10))
    println(danielsCart.run(ShoppingCart(List(), 0)).value)
  }
}

package part2abstractmath

object Monoids {

  import cats.Semigroup
  import cats.instances.int._
  import cats.syntax.semigroup._
  val numbers = (1 to 1000).toList
  // |+| is always associative
  val sumLeft = numbers.foldLeft(0)(_ |+| _)
  val sumRight = numbers.foldRight(0)(_ |+| _)

  // define a general API
  //def combineFold[T](list: List[T])(implicit semigroup: Semigroup[T]): T = list.foldLeft(0)(_ |+| _)

  // MONOIDS
  import cats.Monoid
  val intMonoid = Monoid[Int]
  val combineInt = intMonoid.combine(23, 99)
  val zero = intMonoid.empty // zero

  import cats.instances.string._
  val emptyString = Monoid[String].empty // "" empty string
  val combineString = Monoid[String].combine("I understand ", "monoids")

  import cats.instances.option._ // construct an implicit Monoid[Option[Int]]
  val emptyOption = Monoid[Option[Int]].empty // None
  val combineOption = Monoid[Option[Int]].combine(Option(2), Option.empty[Int])
  val combineOption2 = Monoid[Option[Int]].combine(Option(3), Option(6))

  // extension method for Monoids - |+|
  val combineOptionFancy = Option(3) |+| Option(7)

  def combineFold[T](list: List[T])(implicit monoid: Monoid[T]): T = list.foldLeft(monoid.empty)(_ |+| _)

  val phonebooks = List(
    Map(
      "Alice" -> 235,
      "Bob" -> 657
    ),
    Map(
      "Charlie" -> 372,
      "Daniel" -> 889
    ),
    Map(
      "Tina" -> 123
    )
  )

  import cats.instances.map._
  val massivePhonebook = combineFold(phonebooks)

  case class ShoppingCart(items: List[String], total: Double)
  implicit val shoppingCarMonoid: Monoid[ShoppingCart] = Monoid.instance(
    ShoppingCart(List(), 0.0),
    (sa, sb) => ShoppingCart(sa.items ++ sb.items, sa.total + sb.total)
  )
  def checkout(shoppingCart: List[ShoppingCart]): ShoppingCart = combineFold(shoppingCart)


  def main(args: Array[String]): Unit = {
    println(sumLeft)
    println(sumRight)
    println(combineFold(numbers))
    println(combineFold(List("I ", "like ", "monoids")))
    println(massivePhonebook)
    println(checkout(
      List(
        ShoppingCart(List("iphone", "shoes"), 799),
        ShoppingCart(List("tv"), 20000),
        ShoppingCart(List(), 0)
      )
    ))
  }
}

package lectures.part2afp

object Monads extends App {
  // Are own Try monad
  trait Attempt[+A] {
    def flatMap[B](f: A => Attempt[B]): Attempt[B]
  }
  object Attempt {
    def apply[A](a: => A): Attempt[A] =
      try {
        Success(a)
      } catch {
        case e: Throwable => Failure(e)
      }
  }

  case class Success[+A](value: A) extends Attempt[A] {
    override def flatMap[B](f: A => Attempt[B]): Attempt[B] =
      try {
        f(value)
      } catch {
        case e: Throwable => Failure(e)
      }
  }
  case class Failure(throwable: Throwable) extends Attempt[Nothing] {
    override def flatMap[B](f: Nothing => Attempt[B]): Attempt[B] = this
  }

  val attempt = Attempt {
    throw new RuntimeException("My own monad, yes")
  }

  println(attempt)

  /*
  * Exercise: Implement a Lazy[T] monad
  * unit/apply
  * flatMap
  *
  * 2) Monads = Unit + flatMap
  *    Monads = unit + map + flatten
  * */

  class Lazy[+A](value: => A) {
    // call by need
    private lazy val internalValue = value
    def use: A = internalValue
    def flatMap[B](f: (=> A) => Lazy[B]): Lazy[B] = f(internalValue)
  }
  object Lazy {
    def apply[A](a: => A): Lazy[A] = new Lazy(a)
  }

  val lazyInstance = Lazy {
    println("Today I don't feel like doing anything")
    42
  }

  val flatMapInstance = lazyInstance.flatMap(x => Lazy {
    10 * x
  })

  val flatMapInstance2 = lazyInstance.flatMap(x => Lazy {
    10 * x
  })
  flatMapInstance.use
  flatMapInstance2.use

}

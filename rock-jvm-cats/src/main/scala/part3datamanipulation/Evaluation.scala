package part3datamanipulation

object Evaluation {

  import cats.Eval
  val instantEval: Eval[Int] = Eval.now {
    println("Computing now!")
    43
  }

  val redoEval: Eval[Int] = Eval.always {
    println("Computing again!")
    56
  }

  val delayedEval = Eval.later {
    println("Computing later!")
    657
  }

  val composedEvaluation: Eval[Int] = instantEval.flatMap(v1 => delayedEval.map(v2 => v1 + v2))
  val anotherComposedEvaluation = for {
    v1 <- instantEval
    v2 <- delayedEval
  } yield v1 + v2 // identical

  val evalEx1 = for {
    a <- delayedEval
    b <- redoEval
    c <- instantEval
    d <- redoEval
  } yield a + b + c + d
  /*
  * Computing now!
  Computing later!
  Computing again!
  Computing again!
  812
  Computing again!
  Computing again!
  812
  * */

  // remember a computed value
  val dontRecompute = redoEval.memoize

  val tutorial = Eval
    .always {
      println("Step 1 ....")
      "put the guitar on your lap"
    }
    .map { (s1) =>
      println("Step 2")
      s"$s1 the put your left hand on the neck"
    }
    .memoize // remember the value up to this point
    .map { step12 =>
      println("Step 3, more complicated")
      s"$step12 then with the right hand strike the strings"
    }

  def defer[T](eval: => Eval[T]): Eval[T] =
    Eval.later(()).flatMap(_ => eval)

  def reverseList[T](list: List[T]): List[T] = {
    if (list.isEmpty) list
    else reverseList(list.tail) :+ list.head
  }

  def reverseEval[T](list: List[T]): Eval[List[T]] =
    if (list.isEmpty) Eval.now(list)
    else defer(reverseEval(list.tail).map(_ :+ list.head))

  def main(args: Array[String]): Unit = {
    println(defer(Eval.now {
      println("Now!")
      43
    }).value)

    println(reverseEval((1 to 100000).toList).value)
  }
}

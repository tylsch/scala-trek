package part3datamanipulation

import scala.annotation.tailrec
import scala.util.Try

object DataValidation {

  import cats.data.Validated
  val aValidValue: Validated[String, Int] = Validated.valid(42) // "right" value
  val anInvalidValue: Validated[String, Int] = Validated.invalid("Something went wrong") // "left" value
  val aTest: Validated[String, Int] = Validated.cond(42 > 39, 99, "meaning of life is too small")

  def testPrime(n: Int): Boolean = {
    @tailrec
    def tailRecPrime(d: Int): Boolean =
      if (d <= 1) true
      else (n % d != 0) && tailRecPrime(d - 1)

    if (n == 0 || n == 1 || n == -1) false
    else tailRecPrime(Math.abs(n / 2))
  }
  def testNumber(n: Int): Either[List[String], Int] = {
    val isNotEven: List[String] = if (n % 2 == 0) List() else List("Number is not even")
    val isNegative: List[String] = if (n >= 0) List() else List("Number must be non-negative")
    val isTooBig: List[String] = if (n <= 100) List() else List("Number must be less than or equal to 100")
    val isNotPrime: List[String] = if (testPrime(n)) List() else List("Number must be a prime")

    if (n % 2 == 0 && n >= 0 && n <= 100 && testPrime(n)) Right(n)
    else Left(isNotEven ++ isNegative ++ isTooBig ++ isNotPrime)
  }

  def validateNumber(n: Int): Validated[List[String], Int] =
    Validated.cond(n % 2 == 0, n, List("Number is not even"))
      .combine(Validated.cond(n >= 0, n, List("Number must be non-negative")))
      .combine(Validated.cond(n <= 100, n, List("Number must be less than or equal to 100")))
      .combine(Validated.cond(testPrime(n), n, List("Number must be a prime")))

  // chain
  aValidValue.andThen(_ => anInvalidValue)
  // test a valid value
  aValidValue.ensure(List("something went wrong"))(_ % 2 == 0)
  // transform
  aValidValue.map(_ + 1)
  aValidValue.leftMap(_.length)
  aValidValue.bimap(_.length, _ + 1)
  // interoperate with stdlib
  val eitherToValidated: Validated[List[String], Int] = Validated.fromEither(Right(42))
  val optionToValidated: Validated[List[String], Int] = Validated.fromOption(None, List("Nothing present here"))
  val tryToValidated: Validated[Throwable, Int] = Validated.fromTry(Try("something".toInt))
  //backwards
  aValidValue.toOption
  aValidValue.toEither

  object FormValidation {
    type FormValidation[T] = Validated[List[String], T]

    def getValue(form: Map[String, String], fieldName: String): FormValidation[String] =
      Validated.fromOption(form.get(fieldName), List(s"The field $fieldName must be specified."))

    def nonBlank(value: String, fieldName: String): FormValidation[String] =
      Validated.cond(value.length > 0, value, List(s"The field $fieldName must not be blank."))

    def emailProperForm(email: String): FormValidation[String] =
      Validated.cond(email.contains("@"), email, List("Email is invalid"))

    def passwordCheck(password: String): FormValidation[String] =
      Validated.cond(password.length >= 10, password, List("Passwords must be at least 10 characters long."))

    def validateForm(form: Map[String, String]): FormValidation[String] =
      getValue(form, "Name").andThen(nonBlank(_, "Name"))
        .combine(getValue(form, "Email").andThen(emailProperForm))
        .combine(getValue(form, "Password").andThen(passwordCheck))
        .map(_ => "User registration form completed")
  }

  import cats.syntax.validated._
  val aValidMeaningOfLife: Validated[List[String], Int] = 42.valid[List[String]]
  val anError: Validated[String, Int] = "Something went wrong".invalid[Int]

  def main(args: Array[String]): Unit = {
    val form = Map(
      "Name" -> "daniel",
      "Email" -> "daniel@rockthejvm.com",
      "Password" -> "Rockthejvm1!"
    )

    println(FormValidation.validateForm(form))
  }

}

package com.rockthejvm.part2effects

import zio.{Cause, IO, Task, UIO, URIO, ZIO, ZIOAppDefault}

import java.io.IOException
import java.net.NoRouteToHostException
import scala.util.{Failure, Success, Try}

object ZIOErrorHandling extends ZIOAppDefault {

  // ZIOs can fail
  val aFailedZio = ZIO.fail("Something went wrong")
  val failedWithThrowable = ZIO.fail(new RuntimeException("Boom!"))
  val failedWithDescription = failedWithThrowable.mapError(_.getMessage)

  // attempt: run an effect that might throw an exception
  val badZIO = ZIO.succeed {
    println("Trying something")
    val string: String = null
    string.length
  } // this is bad

  // use attempt if you're every unsure whether your code might throw
  val anAttempt: Task[Int] = ZIO.attempt {
    println("Trying something")
    val string: String = null
    string.length
  }

  // effectually catch error
  val catchError = anAttempt.catchAll(e => ZIO.succeed(s"Returning a different value because ${e}"))
  val catchSelectiveErrors = anAttempt.catchSome {
    case e: RuntimeException => ZIO.succeed(s"Ignoring runtime exceptions: ${e}")
    case _ => ZIO.succeed("Ignoring everything else")
  }

  // chain effects
  val aBetterAttempt = anAttempt.orElse(ZIO.succeed(56))
  // fold: handle both success and failure
  val handleBoth: URIO[Any, String] = anAttempt.fold(ex => s"Something bad happened: $ex", value => s"Length of the string was $value")
  // effectful fold: foldZIO
  val handleBoth_v2 = anAttempt.foldZIO(
    ex => ZIO.succeed(s"Something bad happened: $ex"),
    value => ZIO.succeed(s"Length of the string was $value")
  )

  /*
  * Conversions between Option/Try/Either to ZIO
  *
  * */

  val aTryZIO: Task[Int] = ZIO.fromTry(Try(42 / 0)) // can fail with Throwable

  // either -> ZIO
  val anEither: Either[Int, String] = Right("Success!")
  val anEitherToZIO = ZIO.fromEither(anEither)
  // ZIO -> ZIO with Either as the value channel
  val eitherZIO = anAttempt.either
  // reverse
  val anAttempt_v2 = eitherZIO.absolve

  // option -> ZIO
  val anOption: ZIO[Any, Option[Nothing], Int] = ZIO.fromOption(Some(42))

  def try2ZIO[A](aTry: Try[A]): Task[A] = aTry match
    case Failure(exception) => ZIO.fail(exception)
    case Success(value) => ZIO.succeed(value)

  def either2ZIO[A, B](anEither: Either[A, B]): ZIO[Any, A, B] = anEither match
    case Left(value) => ZIO.fail(value)
    case Right(value) => ZIO.succeed(value)

  def option2ZIO[A](anOption: Option[A]): ZIO[Any, Option[A], A] = anOption match
    case Some(value) => ZIO.succeed(value)
    case None => ZIO.fail(None)

  def zio2zioEither[R, A, B](zio: ZIO[R, A, B]): ZIO[R, Nothing, Either[A, B]] = zio.foldZIO(
    error => ZIO.succeed(Left(error)),
    value => ZIO.succeed(Right(value))
  )

  def absolveZio[R, A, B](zio: ZIO[R, Nothing, Either[A, B]]): ZIO[R, A, B] = zio.flatMap {
    case Left(value) => ZIO.fail(value)
    case Right(value) => ZIO.succeed(value)
  }

  /*
  * Errors = failures presents in the ZIO type signature ("checked" errors)
  * Defects = failures that are unrecoverable, unforeseen, NOT present in the ZIO type signature
  *
  * ZIO[R, E, A] can finish with Exit[E, A]
  * - Success[A] containing a value
  * - Cause[E]
  * -- Fail[E] containing the error
  * -- Die(t: Throwable) which was foreseen
  *
  * */

  val divisionByZero: UIO[Int] = ZIO.succeed(1 / 0)

  val failedInt: ZIO[Any, String ,Int] = ZIO.fail("I failed!")
  val failureCausedExposed: ZIO[Any, Cause[String], Int] = failedInt.sandbox
  val failureCauseHidden: ZIO[Any, String, Int] = failureCausedExposed.unsandbox
  // fold with cause
  val foldedWithCause = failedInt.foldCause(cause => s"this failed with ${cause.defects}", value => s"This succeeded with $value")
  val foldedWithCause_v2 = failedInt.foldCauseZIO(cause => ZIO.succeed(s"this failed with ${cause.defects}"), value => ZIO.succeed(s"This succeeded with $value"))

  /*
    Good practice:
    - at a lower level, your "errors" should be treated
    - at a higher level, you should hide "errors" and assume they are unrecoverable
   */

  def callHttpEndpoint(url: String): ZIO[Any, IOException, String] =
    ZIO.fail(new IOException("no internet, dummy!"))

  val endpointCallWithDefects: ZIO[Any, Nothing, String] = callHttpEndpoint("rockthejvm.com").orDie

  // refining the error channel
  def callHttpEndpointWideError(url: String): ZIO[Any, Exception, String] =
    ZIO.fail(new IOException("no internet!!"))

  def callHttpEndpoint_v2(url: String): ZIO[Any, IOException, String] =
    callHttpEndpointWideError(url).refineOrDie[IOException] {
      case e: IOException => e
      case _: NoRouteToHostException => new IOException(s"No route to hose to $url, can't fetch page")
    }

  // reverse: turn defects into the error channel
  val endpointCallWithError = endpointCallWithDefects.unrefine {
    case e => e.getMessage
  }

  /*
    Combine effects with different errors
   */

  case class IndexError(message: String)
  case class DbError(message: String)
  val callApi: ZIO[Any, IndexError, String] = ZIO.succeed("page: <html></html>")
  val queryDb: ZIO[Any, DbError, Int] = ZIO.succeed(0)
  val combined: ZIO[Any, IndexError | DbError, (String, Int)] =
    for
      page <- callApi
      rows <- queryDb
    yield (page, rows) // lost type safety
  /*
    Solutions:
      - design an error model
      - use Scala 3 union types
      - .mapError to some common error type
  */

  // 1 - make this effect fail with a TYPED error
  val aBadFailure: ZIO[Any, Nothing, Int] = ZIO.succeed[Int](throw new RuntimeException("this is bad!"))
  val aBetterFailure: ZIO[Any, Cause[Nothing], Int] = aBadFailure.sandbox // exposes the defect in the Cause
  val aBetterFailure_v2: ZIO[Any, Throwable, Int] = aBadFailure.unrefine { // surfaces out the exception in the error channel
    case e => e
  }

  // 2 - transform a zio into another zio with a narrower exception type
  def ioException[R, A](zio: ZIO[R, Throwable, A]): ZIO[R, IOException, A] =
    zio.refineOrDie {
      case ioe: IOException => ioe
    }

  // 3
  def left[R, E, A, B](zio: ZIO[R, E, Either[A, B]]): ZIO[R, Either[E, A], B] =
    zio.foldZIO(
      e => ZIO.fail(Left(e)),
      either => either match {
        case Left(a) => ZIO.fail(Right(a))
        case Right(b) => ZIO.succeed(b)
      }
    )

  // 4
  val database = Map(
    "daniel" -> 123,
    "alice" -> 789
  )
  case class QueryError(reason: String)
  case class UserProfile(name: String, phone: Int)

  def lookupProfile(userId: String): ZIO[Any, QueryError, Option[UserProfile]] =
    if (userId != userId.toLowerCase())
      ZIO.fail(QueryError("user ID format is invalid"))
    else
      ZIO.succeed(database.get(userId).map(phone => UserProfile(userId, phone)))

  // surface out all the failed cases of this API
  def betterLookupProfile(userId: String): ZIO[Any, Option[QueryError], UserProfile] =
    lookupProfile(userId).foldZIO(
      error => ZIO.fail(Some(error)),
      profileOption => profileOption match {
        case Some(profile) => ZIO.succeed(profile)
        case None => ZIO.fail(None)
      }
    )

  def betterLookupProfile_v2(userId: String): ZIO[Any, Option[QueryError], UserProfile] =
    lookupProfile(userId).some


  override def run: ZIO[Any, Any, Any] = ???
}

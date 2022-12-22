package com.rockthejvm.part3concurrency

import cats.effect.{IO, IOApp}
import scala.concurrent.duration._

object CancellingIOs extends IOApp.Simple {
  import com.rockthejvm.utils._
  /*
  * Cancelling IOs
  * - fib.cancel
  * - racing & other apis
  * - manual cancellation
  * */
  val chainOfIOs: IO[Int] = IO("waiting").debugM >> IO.canceled >> IO(42).debugM

  // uncancelable
  // exmaple: online store, payment processor
  // payment process MUST NOT be cancelled
  val paymentSystem =
  (
    IO("Payment running, don't cancel me").debugM >>
    IO.sleep(1.second) >>
    IO("payment completed").debugM
  ).onCancel(IO("MEGA CANCEL OF DOOM!").debugM.void)

  val cancellationOfDoom = for {
    fib <- paymentSystem.start
    _ <- IO.sleep(500.millis) >> fib.cancel
    _ <- fib.join
  } yield ()

  val atomicPayment = IO.uncancelable(_ => paymentSystem) // "masking"
  val atomicPayment_v2 = paymentSystem.uncancelable // same

  val noCancellationOfDoom = for {
    fib <- atomicPayment.start
    _ <- IO.sleep(500.millis) >> IO("attempting cancellation...").debugM >> fib.cancel
    _ <- fib.join
  } yield ()

  /*
  * The uncancelable API is more complex and more general.
  * It takes a function from Poll[IO] to IO.  In the example above, we aren't using that Poll instance.
  * The Poll object can be used to mark sections within the returned effect which CAN BE CANCELED.
  * */

  val inputPassword = IO("Input password:").debugM >> IO("typing password").debugM >> IO.sleep(2.seconds) >> IO("RockTheJVM1!")
  val verifyPassword = (pw: String) => IO("verifying...").debugM >> IO.sleep(2.seconds) >> IO(pw == "RockTheJVM1!")

  val authFlow: IO[Unit] = IO.uncancelable { poll =>
    for {
      in <- poll(inputPassword).onCancel(IO("Authentication Timed Out.  Try again later.").debugM.void) // this is cancelable
      verified <- verifyPassword(in) // this is NOT cancelable
      _ <- if (verified) IO("Authentication successful.").debugM else IO("Authentication failed.").debugM // this is NOT cancelable
    } yield ()
  }

  val authProgram = for {
    authFib <- authFlow.start
    _ <- IO.sleep(3.seconds) >> IO("Authentication timeout, attempting cancel...").debugM >> authFib.cancel
    _ <- authFib.join
  } yield ()

  /*
  * Uncancelable calls are MASKS which suppress cancellation
  * Poll calls "gaps opened" in the uncancelable region
  * */

  override def run: IO[Unit] = authProgram
}

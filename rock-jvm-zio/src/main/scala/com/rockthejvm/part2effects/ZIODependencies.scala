package com.rockthejvm.part2effects

import zio.*

import java.io.IOException
import java.util.concurrent.TimeUnit

object ZIODependencies extends ZIOAppDefault:

  // app to subscribe users to newsletter
  import ServiceModel.*

  val subscriptionService = ZIO.succeed( // dependency injection
    UserSubscription.create(
      EmailService.create(),
      UserDatabase.create(
        ConnectionPool.create(10)
      )
    )
  )

  /*
    drawbacks
    - does not scale for many services
    - DI can be 100x worse
      - pass dependencies partially
      - not having all deps in the same place
      - passing dependencies multiple times
   */

  def subscribe(user: User): ZIO[Any, Throwable, Unit] =
    for
      sub <- subscriptionService // service is instantiated at the point of call
      _ <- sub.subscribeUser(user)
    yield ()

  // risk leaking resources if you subscribe multiple users in the same program
  val program =
    for
      _ <- subscribe(User("Daniel", "daniel@rockthejvm.com"))
      _ <- subscribe(User("Bon Jovi", "jon@rockthejvm.com"))
    yield ()

  // alternative
  def subscribe_v2(user: User): ZIO[UserSubscription, Throwable, Unit] =
    for
      sub <- ZIO.service[UserSubscription] // ZIO[UserSubscription, Nothing, UserSubscription]
      _ <- sub.subscribeUser(user)
    yield ()

  val program_v2: ZIO[UserSubscription, Throwable, Unit] =
    for
      _ <- subscribe_v2(User("Daniel", "daniel@rockthejvm.com"))
      _ <- subscribe_v2(User("Bon Jovi", "jon@rockthejvm.com"))
    yield ()

  /*
    - we don't need to care about dependencies until the end of the world
    - all ZIOs requiring this dependency will use the same instance
    - can use different instances of the same type for different needs (e.g. testing)
    - layers can be created and composed much like regular ZIOs + rich API
   */

  /**
   * ZLayers
   */

  val connectionPoolLayer: ULayer[ConnectionPool] = ZLayer.succeed(ConnectionPool.create(10))
  //
  val databaseLayer: ZLayer[ConnectionPool, Nothing, UserDatabase] = ZLayer.fromFunction(UserDatabase.create _)
  val emailServiceLayer: ULayer[EmailService] = ZLayer.succeed(EmailService.create())
  val userSubscriptionServiceLayer: ZLayer[UserDatabase with EmailService, Nothing, UserSubscription] =
    ZLayer.fromFunction(UserSubscription.create _)

  // composing layers
  // vertical composition
  val databaseLayerFull: ZLayer[Any, Nothing, UserDatabase] = connectionPoolLayer >>> databaseLayer
  // horizontal composition: combines the dependencies of both layers AND the values of both layers
  val subscriptionRequirementsLayer: ZLayer[Any, Nothing, UserDatabase with EmailService] = databaseLayerFull ++ emailServiceLayer
  // mix & match
  val userSubscriptionLayer: ZLayer[Any, Nothing, UserSubscription] =
    subscriptionRequirementsLayer >>> userSubscriptionServiceLayer

  // best practice: write "factory" methods exposing layers in the companion objects of the services
  val runnableProgram = program_v2.provideLayer(userSubscriptionLayer)

  // magic
  val runnableProgram_v2 = program_v2.provide(
    UserSubscription.live,
    EmailService.live,
    UserDatabase.live,
    ConnectionPool.live(10),
    // ZIO will tell you if you are missing a layer
    // and if you have multiple layers of the same type
    // ZLayer.Debug.tree,
    ZLayer.Debug.mermaid
  )

  // magic v2
  val userSubscriptionLayer_v2: ZLayer[Any, Nothing, UserSubscription] = ZLayer.make[UserSubscription](
    UserSubscription.live,
    EmailService.live,
    UserDatabase.live,
    ConnectionPool.live(10)
  )

  // pass-through
  val dbWithPoolLayer: ZLayer[ConnectionPool, Nothing, ConnectionPool with UserDatabase] = UserDatabase.live.passthrough
  // service = take a dependency and expose it as a value to further layers
  val dbService = ZLayer.service[UserDatabase]
  // launch = creates a ZIO that uses the services and never finishes
  val subscriptionLaunch: ZIO[EmailService with UserDatabase, Nothing, Nothing] = UserSubscription.live.launch
  // memoization

  /*
    Already provided services: Clock, Random, System, Console
   */
  val getTime: UIO[Long] = Clock.currentTime(TimeUnit.SECONDS)
  val randomValue: UIO[RuntimeFlags] = Random.nextInt
  val sysVariable: IO[SecurityException, Option[String]] = System.env("HADOOP_HOME")
  val printlnEffect: IO[IOException, Unit] = Console.printLine("This is ZIO")

  override def run: ZIO[Any, Any, Any] = runnableProgram_v2


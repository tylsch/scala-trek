package part4infra

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import com.typesafe.config.ConfigFactory

object AkkaConfiguration {

  object SimpleLoggingActor {
    def apply(): Behavior[String] = Behaviors.receive { (ctx, msg) =>
      ctx.log.info(msg)
      Behaviors.same
    }
  }

  // 1 - inline
  def demoInlineConfig(): Unit = {
    // HOCON, superset of JSON, managed by Lightbend
    val configString: String =
      """
        |akka {
        |  loglevel = "DEBUG"
        |}
        |""".stripMargin

    val config = ConfigFactory.parseString(configString)
    val system = ActorSystem(SimpleLoggingActor(), "ConfigDemo", config)

    system ! "A message to remember"
    Thread.sleep(1000)
    system.terminate()
  }

  // 2 - config file
  def demoConfigFile(): Unit = {
    val specialConfig = ConfigFactory.load().getConfig("mySpecialConfig2")
    val system = ActorSystem(SimpleLoggingActor(), "ConfigDemo", specialConfig)

    system ! "A message to remember"
    Thread.sleep(1000)
    system.terminate()
  }

  // 3 - a different config in another file
  def demoSeparateConfigFile(): Unit = {
    val specialConfig = ConfigFactory.load("secretDir/secretConfiguration.conf")
    println(specialConfig.getString("akka.loglevel"))
  }

  // 4 - different file formats (JSON, properties)
  def demoOtherFileFormats(): Unit = {
    val specialConfig = ConfigFactory.load("json/jsonConfiguration.json")
    println(specialConfig.getString("aJsonProperty"))
    println(specialConfig.getString("akka.loglevel"))

    // properties format
    val propConfig = ConfigFactory.load("properties/propsConfiguration.properties")
    println(propConfig.getString("mySimpleProperty"))
    println(propConfig.getString("akka.loglevel"))
  }

  def main(args: Array[String]): Unit = {
    demoOtherFileFormats()
  }
}

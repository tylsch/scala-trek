import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.pattern.StatusReply
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.wordspec.AnyWordSpecLike
import shopping.cart.ShoppingCart

object ShoppingCartSpec {
  val config: Config = ConfigFactory.parseString("""
      |akka.actor.serialization-bindings {
      |  "shopping.cart.CborSerializable" = jackson-cbor
      |}
      |""".stripMargin)
    .withFallback(EventSourcedBehaviorTestKit.config)
}

class ShoppingCartSpec
  extends ScalaTestWithActorTestKit(ShoppingCartSpec.config)
  with AnyWordSpecLike
  with BeforeAndAfterEach {

  private val cartId = "testCart"
  private val eventSourcedTestKit =
    EventSourcedBehaviorTestKit[
      ShoppingCart.Command,
    ShoppingCart.Event,
    ShoppingCart.State](system, ShoppingCart(cartId))

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  "The Shopping Cart" should {
    "add item" in {
      var result1 = eventSourcedTestKit.runCommand[StatusReply[ShoppingCart.Summary]](
        replyTo => ShoppingCart.AddItem("foo", 42, replyTo)
      )

      result1.reply should ===(StatusReply.Success(ShoppingCart.Summary(Map("foo" -> 42))))
      result1.event should ===(ShoppingCart.ItemAdded(cartId, "foo", 42))
    }
    "reject already added item" in {
      var result1 = eventSourcedTestKit.runCommand[StatusReply[ShoppingCart.Summary]](
        replyTo => ShoppingCart.AddItem("foo", 42, replyTo)
      )
      var result2 = eventSourcedTestKit.runCommand[StatusReply[ShoppingCart.Summary]](
        replyTo => ShoppingCart.AddItem("foo", 42, replyTo)
      )

      result1.reply should ===(StatusReply.Success(ShoppingCart.Summary(Map("foo" -> 42))))
      result2.reply should ===(StatusReply.Error("Item 'foo' was already added to this shopping cart"))
    }
    "reject item where quantity is less than zero" in {
      var result = eventSourcedTestKit.runCommand[StatusReply[ShoppingCart.Summary]](
        replyTo => ShoppingCart.AddItem("foo", 0, replyTo)
      )

      result.reply should ===(StatusReply.Error("Quantity must be greater than zero"))
    }
  }
}

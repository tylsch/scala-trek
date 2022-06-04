import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.pattern.StatusReply
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterEach
import org.scalatest.wordspec.AnyWordSpecLike
import shopping.cart.ShoppingCart

object ShoppingCartSpec {
  val config = ConfigFactory.parseString("""
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
  }
}

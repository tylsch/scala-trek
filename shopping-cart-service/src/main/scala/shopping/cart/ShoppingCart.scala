package shopping.cart

import akka.actor.typed.{ActorRef, ActorSystem, Behavior, SupervisorStrategy}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object ShoppingCart {
  sealed trait Command extends CborSerializable
  sealed trait Event extends CborSerializable {
    def cartId: String
  }

  final case class State(items: Map[String, Int]) extends CborSerializable {
    def hasItem(itemId: String): Boolean =
      items.contains(itemId)

    def isEmpty: Boolean =
      items.isEmpty

    def updateItem(itemId: String, quantity: Int): State = {
      quantity match {
        case 0 => copy(items = items - itemId)
        case _ => copy(items = items + (itemId -> quantity))
      }
    }
  }
  object State {
    val empty: State = State(items = Map.empty)
  }

  final case class AddItem(
      itemId: String,
      quantity: Int,
      replyTo: ActorRef[StatusReply[Summary]])
  extends Command

  final case class Summary(items: Map[String, Int]) extends CborSerializable

  final case class ItemAdded(cartId: String, itemId: String, quantity: Int) extends Event

  val EntityKey: EntityTypeKey[Command] =
    EntityTypeKey[Command]("ShoppingCart")

  def init(system: ActorSystem[_]): Unit = {
    ClusterSharding(system).init(Entity(EntityKey) { entityContext =>
      ShoppingCart(entityContext.entityId)
    })
  }

  def apply(cartId: String): Behavior[Command] =
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, State](
        persistenceId = PersistenceId(EntityKey.name, cartId),
        emptyState = State.empty,
        commandHandler = (state, command) => handleCommand(cartId, state, command),
        eventHandler =  (state, event) => handleEvent(state, event))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 3))
      .onPersistFailure(SupervisorStrategy.restartWithBackoff(200 millis, 5 seconds, 0.1))

  def handleCommand(cartId: String, state: State, command: Command): ReplyEffect[Event, State] = {
    // TODO: Add guards to check duplicate items and quantity
    command match {
      case AddItem(itemId, quantity, replyTo) =>
        Effect
          .persist(ItemAdded(cartId, itemId, quantity))
          .thenReply(replyTo) { updatedCart =>
            StatusReply.Success(Summary(updatedCart.items))
          }
    }
  }

  def handleEvent(state: State, event: Event): State = {
    event match {
      case ItemAdded(_, itemId, quantity) =>
        state.updateItem(itemId, quantity)
    }
  }
}

package EShop.lab3

import EShop.lab2.{TypedCartActor, TypedCheckout}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

object OrderManager {

  sealed trait Command

  case class AddItem(id: String, sender: ActorRef[Ack]) extends Command

  case class RemoveItem(id: String, sender: ActorRef[Ack]) extends Command

  case class SelectDelivery(delivery: String, sender: ActorRef[Ack]) extends Command
  case class SelectPayment(payment: String, sender: ActorRef[Ack]) extends Command

  case class Buy(sender: ActorRef[Ack]) extends Command

  case class Pay(sender: ActorRef[Ack]) extends Command

  case class ConfirmCheckoutStarted(checkoutRef: ActorRef[TypedCheckout.Command]) extends Command

  case class ConfirmPaymentStarted(paymentRef: ActorRef[TypedPayment.Command]) extends Command

  case object ConfirmPaymentReceived extends Command

  case object ConfirmCheckOutClosed extends Command

  sealed trait Ack

  case object Done extends Ack //trivial ACK
}

class OrderManager {

  private var paymentMapper: ActorRef[TypedPayment.Event] = _
  private var typedCartMapper: ActorRef[TypedCartActor.Event] = _
  private var typedCheckoutMapper: ActorRef[TypedCheckout.Event] = _

  import OrderManager._

  def start: Behavior[OrderManager.Command] = Behaviors.setup { context =>
    val typedCartActor = context.spawnAnonymous(new TypedCartActor().empty)
    paymentMapper = context.messageAdapter[TypedPayment.Event] {
      case TypedPayment.PaymentReceived => ConfirmPaymentReceived
    }
    typedCartMapper = context.messageAdapter[TypedCartActor.Event] {
      case TypedCartActor.CheckoutStarted(checkoutRef) => ConfirmCheckoutStarted(checkoutRef)
    }
    typedCheckoutMapper = context.messageAdapter[TypedCheckout.Event] {
      case TypedCheckout.PaymentStarted(paymentRef) => ConfirmPaymentStarted(paymentRef)
      case TypedCheckout.CheckOutClosed => ConfirmCheckOutClosed
    }
    open(cartActor = typedCartActor)
  }

  def open(cartActor: ActorRef[TypedCartActor.Command]): Behavior[OrderManager.Command] =
    Behaviors.receive(
      (context, message) => message match {
        case AddItem(id, sender) =>
          cartActor ! TypedCartActor.AddItem(id)
          println("OM: Adds item")
          sender ! Done
          Behaviors.same
        case RemoveItem(id, sender) =>
          cartActor ! TypedCartActor.RemoveItem(id)
          sender ! Done
          Behaviors.same
        case Buy(sender) =>
          println("OM: Buys")
          cartActor ! TypedCartActor.StartCheckout(orderManagerRef = context.self)
          inCheckout(cartActorRef = cartActor, senderRef = sender)
      }
    )

  def inCheckout(cartActorRef: ActorRef[TypedCartActor.Command], senderRef: ActorRef[Ack]): Behavior[OrderManager.Command] =
    Behaviors.receiveMessage {
      case ConfirmCheckoutStarted(checkoutRef) =>
        senderRef ! Done
        inCheckout(checkoutActorRef = checkoutRef)
    }

  def inCheckout(checkoutActorRef: ActorRef[TypedCheckout.Command]): Behavior[OrderManager.Command] =
    Behaviors.receive(
      (context, message) => message match {
        case SelectDelivery(deliveryMethod, sender) =>
          println("OM: Select Delivery")
          checkoutActorRef ! TypedCheckout.SelectDeliveryMethod(method = deliveryMethod)
          sender ! Done
          Behaviors.same
        case SelectPayment(paymentMethod, sender) =>
          println("OM: Select Payment")
          checkoutActorRef ! TypedCheckout.SelectPayment(
            payment = paymentMethod,
            orderManagerPaymentEventHandler = paymentMapper,
            orderManagerCheckoutEventHandler = typedCheckoutMapper
          )
          inPayment(senderRef = sender)
      }
    )
  private def inPayment(senderRef: ActorRef[Ack]): Behavior[OrderManager.Command] =
    Behaviors.receiveMessage {
      case ConfirmPaymentStarted(paymentRef) =>
        println("confirm payment started")
        senderRef ! Done
        inPayment(paymentActorRef = paymentRef, senderRef = senderRef)
      case ConfirmPaymentReceived =>
        println("Payment received")
        senderRef ! Done
        finished
    }

  private def inPayment(paymentActorRef: ActorRef[TypedPayment.Command], senderRef: ActorRef[Ack]): Behavior[OrderManager.Command] =
    Behaviors.receiveMessage {
      case Pay(sender) =>
        println("Paying")
        paymentActorRef ! TypedPayment.DoPayment
        sender ! Done
        inPayment(senderRef = sender)
  }

  private def finished: Behavior[OrderManager.Command] = Behaviors.stopped
}
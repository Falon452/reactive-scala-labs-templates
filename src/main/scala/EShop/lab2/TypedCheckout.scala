package EShop.lab2

import EShop.lab3.TypedPayment
import akka.actor.Cancellable
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}

import scala.concurrent.duration._
import scala.language.postfixOps

object TypedCheckout {

  sealed trait Data

  sealed trait Command
  case object StartCheckout                       extends Command
  case class SelectDeliveryMethod(method: String) extends Command
  case object CancelCheckout                      extends Command
  case object ExpireCheckout                      extends Command

  case class SelectPayment(payment: String,
                           orderManagerPaymentEventHandler: ActorRef[TypedPayment.Event],
                           orderManagerCheckoutEventHandler: ActorRef[TypedCheckout.Event]) extends Command
  case object ExpirePayment                       extends Command
  case object ConfirmPaymentReceived              extends Command

  sealed trait Event

  case object CheckOutClosed extends Event

  case class PaymentStarted(paymentRef: ActorRef[TypedPayment.Command]) extends Event
}

class TypedCheckout {
  import TypedCheckout._

  var deliveryMethod = ""
  var paymentMethod = ""
  private val checkoutTimerDuration: FiniteDuration = 10 seconds
  private val paymentTimerDuration: FiniteDuration  = 10 seconds
  var paymentMapper: ActorRef[TypedPayment.Event] = _

  private def checkoutTimer(context: ActorContext[Command]): Cancellable =
    context.scheduleOnce(checkoutTimerDuration, context.self, ExpireCheckout)

  private def paymentTimer(context: ActorContext[Command]): Cancellable =
    context.scheduleOnce(paymentTimerDuration, context.self, ExpirePayment)
  def start: Behavior[TypedCheckout.Command] = Behaviors.receive(
    (context, msg) => msg match {
      case StartCheckout =>
        selectingDelivery(checkoutTimer(context))
    }
  )

  private def selectingDelivery(timer: Cancellable): Behavior[TypedCheckout.Command] = Behaviors.receive(
    (context, msg) => msg match {
      case SelectDeliveryMethod(method: String) =>
        println("selecting payment method")
        this.deliveryMethod = method
        timer.cancel()
        selectingPaymentMethod(checkoutTimer(context))

      case ExpireCheckout =>
        timer.cancel()
        println("Checkout expired")
        cancelled

      case CancelCheckout =>
        timer.cancel()
        cancelled
    }
  )

  private def selectingPaymentMethod(timer: Cancellable): Behavior[TypedCheckout.Command] = Behaviors.receive(
    (context, msg) => msg match {
      case SelectPayment(method: String, orderManagerPaymentEventHandler, orderManagerCheckoutEventHandler) =>
        println("Select payment checkout")
        this.paymentMethod = method
        timer.cancel()

        val payment = new TypedPayment(method, orderManagerPaymentEventHandler, paymentMapper)

        val paymentRef = context.spawn(payment.start, "payment")
        orderManagerCheckoutEventHandler ! TypedCheckout.PaymentStarted(paymentRef = paymentRef)
        processingPayment(paymentTimer(context))

      case ExpireCheckout =>
        timer.cancel()
        println("Checkout expired")
        cancelled

      case CancelCheckout =>
        timer.cancel()
        cancelled
    }
  )

  private def processingPayment(timer: Cancellable): Behavior[TypedCheckout.Command] = Behaviors.receive(
    (_, msg) => msg match {
      case ConfirmPaymentReceived =>
        timer.cancel()
        closed

      case ExpirePayment =>
        timer.cancel()
        println("Payment expired")
        cancelled

      case CancelCheckout =>
        timer.cancel()
        cancelled
    }
  )

  private def cancelled: Behavior[TypedCheckout.Command] = Behaviors.receive(
    (_, _) => {
      Behaviors.same
    }
  )

  private def closed: Behavior[TypedCheckout.Command] = Behaviors.receive(
    (_, _) => {
      println("Checkout closed")
      Behaviors.same
    }
  )

}

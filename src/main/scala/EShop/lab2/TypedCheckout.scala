package EShop.lab2

import akka.actor.Cancellable
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}

import scala.concurrent.duration._
import scala.language.postfixOps

object TypedCheckout {

  sealed trait Data
  case object Uninitialized                               extends Data
  case class SelectingDeliveryStarted(timer: Cancellable) extends Data
  case class ProcessingPaymentStarted(timer: Cancellable) extends Data

  sealed trait Command
  case object StartCheckout                       extends Command
  case class SelectDeliveryMethod(method: String) extends Command
  case object CancelCheckout                      extends Command
  case object ExpireCheckout                      extends Command
  case class SelectPayment(payment: String)       extends Command
  case object ExpirePayment                       extends Command
  case object ConfirmPaymentReceived              extends Command

  sealed trait Event
  case object CheckOutClosed                        extends Event
  case class PaymentStarted(payment: ActorRef[Any]) extends Event
}

class TypedCheckout {
  import TypedCheckout._

  var deliveryMethod = ""
  var paymentMethod = ""
  val checkoutTimerDuration: FiniteDuration = 1 seconds
  val paymentTimerDuration: FiniteDuration  = 1 seconds

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

  def selectingDelivery(timer: Cancellable): Behavior[TypedCheckout.Command] = Behaviors.receive(
    (context, msg) => msg match {
      case SelectDeliveryMethod(method: String) =>
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

  def selectingPaymentMethod(timer: Cancellable): Behavior[TypedCheckout.Command] = Behaviors.receive(
    (context, msg) => msg match {
      case SelectPayment(method: String) =>
        this.paymentMethod = method
        timer.cancel()
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

  def processingPayment(timer: Cancellable): Behavior[TypedCheckout.Command] = Behaviors.receive(
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

  def cancelled: Behavior[TypedCheckout.Command] = Behaviors.receive(
    (_, _) => {
      Behaviors.same
    }
  )

  def closed: Behavior[TypedCheckout.Command] = Behaviors.receive(
    (_, _) => {
      println("Checkout closed")
      Behaviors.same
    }
  )

}

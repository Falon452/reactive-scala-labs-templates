package EShop.lab2

import EShop.lab2.Checkout._
import akka.actor.{Actor,ActorRef, Cancellable}
import akka.event.Logging

import scala.concurrent.duration._
import scala.language.postfixOps

object Checkout {
  sealed trait Command
  case object StartCheckout                       extends Command
  case class SelectDeliveryMethod(method: String) extends Command
  case object CancelCheckout                      extends Command
  case object ExpireCheckout                      extends Command
  case class SelectPayment(payment: String)       extends Command
  case object ExpirePayment                       extends Command
  case object ConfirmPaymentReceived              extends Command

  sealed trait Event
  case object CheckOutClosed                   extends Event
  case class PaymentStarted(payment: ActorRef) extends Event

}

class Checkout extends Actor {
  import context._

  private val scheduler = context.system.scheduler
  private val log       = Logging(context.system, this)

  val checkoutTimerDuration = 1 seconds
  val paymentTimerDuration  = 1 seconds

  private def expireCheckoutTimer: Cancellable =
    system.scheduler.scheduleOnce(checkoutTimerDuration, self, ExpireCheckout)

  private def expirePaymentTimer: Cancellable =
    system.scheduler.scheduleOnce(paymentTimerDuration, self, ExpirePayment)

  def receive: Receive = {
    case StartCheckout =>
      context become selectingDelivery(expireCheckoutTimer)
  }

  def selectingDelivery(timer: Cancellable): Receive = {
    case SelectDeliveryMethod(method: String) =>
      println(method)
      timer.cancel()
      context become selectingPaymentMethod(expireCheckoutTimer)

    case ExpireCheckout =>
      timer.cancel()
      println("ExpireCheckout")
      context become cancelled

    case CancelCheckout =>
      timer.cancel()
      context become cancelled
  }

  def selectingPaymentMethod(timer: Cancellable): Receive = {
    case SelectPayment(method: String) =>
      print("Selected method: ")
      println(method)
      timer.cancel()
      context become processingPayment(expirePaymentTimer)

    case ExpireCheckout =>
      timer.cancel()
      println("ExpireCheckout")
      context become cancelled

    case CancelCheckout =>
      timer.cancel()
      context become cancelled
  }

  def processingPayment(timer: Cancellable): Receive = {
    case ConfirmPaymentReceived =>
      timer.cancel()
      context become closed

    case ExpirePayment =>
      timer.cancel()
      println("ExpirePayment")
      context become cancelled

    case CancelCheckout =>
      timer.cancel()
      context become cancelled
  }

  def cancelled: Receive = {
    _ => context.stop(self)
  }

  def closed: Receive = {
    _ =>
      println("closed")
      context.stop(self)
  }

}

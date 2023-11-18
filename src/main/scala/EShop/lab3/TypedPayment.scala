package EShop.lab3

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

object TypedPayment {

  sealed trait Command

  case object DoPayment extends Command

  sealed trait Event

  case object PaymentReceived extends Event
}

class TypedPayment(
                    method: String,
                    orderManagerEventHandler: ActorRef[TypedPayment.Event],
                    checkoutEventHandler: ActorRef[TypedPayment.Event]
                  ) {

  import TypedPayment._

  def start: Behavior[TypedPayment.Command] = Behaviors.receive((_, msg) => msg match {
    case DoPayment =>
      println(method)
      orderManagerEventHandler ! PaymentReceived
      checkoutEventHandler ! PaymentReceived
      Behaviors.stopped
  })
}

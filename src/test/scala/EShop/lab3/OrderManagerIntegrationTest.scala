package EShop.lab3


import EShop.lab3.OrderManager.{AddItem, Buy, Done, Pay, SelectDelivery, SelectPayment}
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.{ActorRef, Scheduler}
import akka.util.Timeout
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._

class OrderManagerIntegrationTest
  extends ScalaTestWithActorTestKit
    with AnyFlatSpecLike
    with BeforeAndAfterAll
    with Matchers
    with ScalaFutures {

  override implicit val timeout: Timeout = 1.second

  implicit val scheduler: Scheduler = testKit.scheduler

  def sendMessage(
                   orderManager: ActorRef[OrderManager.Command],
                   message: ActorRef[Any] => OrderManager.Command
                 ): Unit = {
    import akka.actor.typed.scaladsl.AskPattern.Askable
    orderManager.ask[Any](message).mapTo[OrderManager.Ack].futureValue shouldBe Done
  }

  it should "supervise whole order process" in {
    val orderManager = testKit.spawn(new OrderManager().start).ref

    sendMessage(orderManager, AddItem("Mac1", _))

    sendMessage(orderManager, Buy)

    sendMessage(orderManager, SelectDelivery("inpost", _))

    println("sending select payment")
    sendMessage(orderManager, SelectPayment("VISA", _))

    sendMessage(orderManager, ref => Pay(ref))
  }
}
package EShop.lab3

import EShop.lab2.TypedCartActor.{AddItem, GetItems, RemoveItem, StartCheckout}
import EShop.lab2.{Cart, TypedCartActor}
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.scaladsl.AskPattern.Askable
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers


class TypedCartTest extends ScalaTestWithActorTestKit with AnyFlatSpecLike with BeforeAndAfterAll with Matchers with ScalaFutures {

  override def afterAll: Unit = testKit.shutdownTestKit()

  it should "WHEN adds item, THEN cart has that item" in {
    // GIVEN
    val item = "test-item"
    val typedCart = testKit.spawn(new TypedCartActor().start).ref
    val probe = testKit.createTestProbe[Cart]()
    val expectedItems = Seq(item)

    // WHEN
    typedCart tell AddItem(item)
    typedCart tell GetItems(sender = probe.ref)

    // THEN
    probe expectMessage Cart(items = expectedItems)
  }

  it should "WHEN adds item AND removes that item, THEN cart is empty" in {
    // GIVEN
    val item = "test-item"
    val typedCart = testKit.spawn(new TypedCartActor().start).ref
    val probe = testKit.createTestProbe[Cart]()
    val expectedItems = Seq()

    // WHEN
    typedCart tell AddItem(item)
    typedCart tell RemoveItem(item)
    typedCart tell GetItems(sender = probe.ref)

    // THEN
    probe expectMessage Cart(items = expectedItems)
  }

  it should "WHEN starts checkout, THEN order manager is informed" in {
    // GIVEN
    val item = "test-item"
    val typedCart = testKit.spawn(new TypedCartActor().start).ref
    val orderManagerProbe = testKit.createTestProbe[OrderManager.Command]()

    // WHEN
    typedCart tell AddItem(item)
    typedCart tell StartCheckout(orderManagerRef = orderManagerProbe.ref)

    // THEN
    orderManagerProbe.expectMessageType[OrderManager.ConfirmCheckoutStarted]
  }
}
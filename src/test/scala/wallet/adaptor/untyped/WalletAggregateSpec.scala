package wallet.adaptor.untyped

import java.time.Instant

import akka.actor.ActorSystem
import akka.testkit.{ ImplicitSender, TestKit, TestProbe }
import org.scalatest._
import wallet.adaptor.untyped.WalletProtocol._
import wallet.domain.Money
import wallet.utils.ULID

import scala.concurrent.duration._

class WalletAggregateSpec
    extends TestKit(ActorSystem("WalletAggregateSpec"))
    with FreeSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ImplicitSender {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "WalletAggregate" - {
    "create" in {
      val walletId  = ULID.generate
      val walletRef = system.actorOf(WalletAggregate.props(walletId, 1 hours))

      walletRef ! CreateWalletRequest(ULID.generate, walletId)
      expectMsg(CreateWalletSucceeded)
    }
    "addSubscribers" in {
      val walletId    = ULID.generate
      val walletRef   = system.actorOf(WalletAggregate.props(walletId, 1 hours))
      val eventProbes = for (_ <- 1 to 5) yield TestProbe()
      walletRef ! AddSubscribers(ULID.generate, walletId, eventProbes.map(_.ref).toVector)

      walletRef ! CreateWalletRequest(ULID.generate, walletId)
      expectMsg(CreateWalletSucceeded)
      eventProbes.foreach { eventProbe =>
        eventProbe.expectMsgType[WalletCreated].walletId shouldBe walletId
      }
    }
    "deposit" in {
      val walletId  = ULID.generate
      val walletRef = system.actorOf(WalletAggregate.props(walletId, 1 hours))

      walletRef ! CreateWalletRequest(ULID.generate, walletId)
      expectMsg(CreateWalletSucceeded)

      val money = Money(BigDecimal(100))
      walletRef ! DepositRequest(ULID.generate, walletId, money, Instant.now)
      expectMsg(DepositSucceeded)
    }
    "request" in {
      val walletId  = ULID.generate
      val walletRef = system.actorOf(WalletAggregate.props(walletId, 1 hours))

      walletRef ! CreateWalletRequest(ULID.generate, walletId)
      expectMsg(CreateWalletSucceeded)

      val requestId = ULID.generate
      val money     = Money(BigDecimal(100))
      walletRef ! RequestRequest(ULID.generate, requestId, walletId, money, Instant.now)
      expectMsg(RequestSucceeded)
    }
  }
}

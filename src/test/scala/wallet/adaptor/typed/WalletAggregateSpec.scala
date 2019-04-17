package wallet.adaptor.typed

import java.time.Instant

import akka.actor.testkit.typed.scaladsl.{ ScalaTestWithActorTestKit, TestProbe }
import org.scalatest._
import wallet.adaptor.typed.WalletProtocol._
import wallet.domain.Money
import wallet.utils.ULID

import scala.concurrent.duration._

class WalletAggregateSpec extends ScalaTestWithActorTestKit with FreeSpecLike with Matchers {

  "WalletAggregate" - {
    "create" in {
      val walletId                  = ULID.generate
      val walletRef                 = spawn(WalletAggregate.behavior(walletId, 1 hours))
      val createWalletResponseProbe = TestProbe[CreateWalletResponse]

      walletRef ! CreateWalletRequest(ULID.generate, walletId, createWalletResponseProbe.ref)
      createWalletResponseProbe.expectMessage(CreateWalletSucceeded)
    }
    "addSubscribers" in {
      val walletId    = ULID.generate
      val walletRef   = spawn(WalletAggregate.behavior(walletId, 1 hours))
      val eventProbes = for (_ <- 1 to 5) yield TestProbe[Event]
      walletRef ! AddSubscribers(ULID.generate, walletId, eventProbes.map(_.ref).toVector)

      val probe = TestProbe[CreateWalletResponse]
      walletRef ! CreateWalletRequest(ULID.generate, walletId, probe.ref)
      probe.expectMessage(CreateWalletSucceeded)
      eventProbes.foreach { eventProbe =>
        eventProbe.expectMessageType[WalletCreated].walletId shouldBe walletId
      }
    }
    "deposit" in {
      val walletId  = ULID.generate
      val walletRef = spawn(WalletAggregate.behavior(walletId, 1 hours))

      val createWalletResponseProbe = TestProbe[CreateWalletResponse]
      walletRef ! CreateWalletRequest(ULID.generate, walletId, createWalletResponseProbe.ref)
      createWalletResponseProbe.expectMessage(CreateWalletSucceeded)

      val money               = Money(BigDecimal(100))
      val depositRequestProbe = TestProbe[DepositResponse]
      walletRef ! DepositRequest(ULID.generate, walletId, money, Instant.now, depositRequestProbe.ref)
      depositRequestProbe.expectMessage(DepositSucceeded)
    }
    "request" in {
      val walletId  = ULID.generate
      val walletRef = spawn(WalletAggregate.behavior(walletId, 1 hours))

      val createWalletResponseProbe = TestProbe[CreateWalletResponse]
      walletRef ! CreateWalletRequest(ULID.generate, walletId, createWalletResponseProbe.ref)
      createWalletResponseProbe.expectMessage(CreateWalletSucceeded)

      val requestId           = ULID.generate
      val money               = Money(BigDecimal(100))
      val requestRequestProbe = TestProbe[RequestResponse]
      walletRef ! RequestRequest(ULID.generate, requestId, walletId, money, Instant.now, requestRequestProbe.ref)
      requestRequestProbe.expectMessage(RequestSucceeded)
    }
  }

}

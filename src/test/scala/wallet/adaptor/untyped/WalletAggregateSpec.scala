package wallet.adaptor.untyped

import java.time.Instant

import akka.testkit.TestProbe
import wallet._
import wallet.adaptor.untyped.WalletProtocol._
import wallet.domain.{ Balance, Money }

/**
  * Wallet集約アクターの単体テスト。
  */
class WalletAggregateSpec extends AkkaSpec {

  "WalletAggregate" - {
    // Walletの作成
    "create" in {
      val walletId  = newULID
      val walletRef = system.actorOf(WalletAggregate.props(walletId))

      walletRef ! CreateWalletRequest(newULID, walletId, Instant.now)
      expectMsg(CreateWalletSucceeded)
    }
    // 入金
    "deposit" in {
      val walletId  = newULID
      val walletRef = system.actorOf(WalletAggregate.props(walletId))

      walletRef ! CreateWalletRequest(newULID, walletId, Instant.now)
      expectMsg(CreateWalletSucceeded)

      val money = Money(BigDecimal(100))
      walletRef ! DepositRequest(newULID, walletId, money, Instant.now)
      expectMsg(DepositSucceeded)
    }
    // 残高確認
    "get balance" in {
      val walletId  = newULID
      val walletRef = system.actorOf(WalletAggregate.props(walletId))

      walletRef ! CreateWalletRequest(newULID, walletId, Instant.now)
      expectMsg(CreateWalletSucceeded)

      val money = Money(BigDecimal(100))
      walletRef ! DepositRequest(newULID, walletId, money, Instant.now)
      expectMsg(DepositSucceeded)

      walletRef ! GetBalanceRequest(newULID, walletId)
      expectMsg(GetBalanceResponse(Balance(Money(BigDecimal(100)))))
    }
    // 請求
    "request" in {
      val walletId  = newULID
      val walletRef = system.actorOf(WalletAggregate.props(walletId))

      walletRef ! CreateWalletRequest(newULID, walletId, Instant.now)
      expectMsg(CreateWalletSucceeded)

      val requestId = newULID
      val money     = Money(BigDecimal(100))
      walletRef ! RequestRequest(newULID, requestId, walletId, newULID, money, Instant.now)
      expectMsg(RequestSucceeded)
    }
    // TODO: 支払い
    "payment" in {
      val payerWalletId  = newULID
      val payerWalletRef = system.actorOf(WalletAggregate.props(payerWalletId))

      payerWalletRef ! CreateWalletRequest(newULID, payerWalletId, Instant.now)
      expectMsg(CreateWalletSucceeded)

      val receiverWalletId  = newULID
      val receiverWalletRef = system.actorOf(WalletAggregate.props(receiverWalletId))

      receiverWalletRef ! CreateWalletRequest(newULID, receiverWalletId, Instant.now)
      expectMsg(CreateWalletSucceeded)

      val payerDeposit = Money(BigDecimal(1000))
      payerWalletRef ! DepositRequest(newULID, payerWalletId, payerDeposit, Instant.now)
      expectMsg(DepositSucceeded)

      val requestId     = newULID
      val requestAmount = Money(BigDecimal(400))
      payerWalletRef ! RequestRequest(newULID, requestId, payerWalletId, receiverWalletId, requestAmount, Instant.now)
      expectMsg(RequestSucceeded)

      payerWalletRef ! PayRequest(
        newULID,
        payerWalletId,
        receiverWalletId,
        requestAmount,
        Some(requestId),
        Instant.now()
      )
      expectMsg(PaySucceeded)

      payerWalletRef ! GetBalanceRequest(newULID, payerWalletId)
      expectMsg(GetBalanceResponse(Balance(Money(BigDecimal(600)))))

      receiverWalletRef ! GetBalanceRequest(newULID, payerWalletId)
      expectMsg(GetBalanceResponse(Balance(Money(BigDecimal(400)))))
    }
    // TODO: 取引履歴の確認
    // akka-persistence-queryを使う
    // ドメインイベントの購読
    "addSubscribers" in {
      val walletId    = newULID
      val walletRef   = system.actorOf(WalletAggregate.props(walletId))
      val eventProbes = for (_ <- 1 to 5) yield TestProbe()
      walletRef ! AddSubscribers(newULID, walletId, eventProbes.map(_.ref).toVector)

      walletRef ! CreateWalletRequest(newULID, walletId, Instant.now)
      expectMsg(CreateWalletSucceeded)
      eventProbes.foreach { eventProbe =>
        eventProbe.expectMsgType[WalletCreated].walletId shouldBe walletId
      }
    }
  }
}

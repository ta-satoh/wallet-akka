package wallet.adaptor.untyped

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import wallet.WalletId
import wallet.adaptor.untyped.WalletProtocol._
import wallet.domain.{ Balance, Money, Wallet }

object WalletAggregate {

  def props(id: WalletId, requestsLimit: Int = Int.MaxValue): Props =
    Props(new WalletAggregate(id, requestsLimit))

  def name(id: WalletId): String = "wallet-untyped-" + id.toString

}

private[untyped] final class WalletAggregate(id: WalletId, requestsLimit: Int) extends Actor with ActorLogging {

  private def getWallet(w: Option[Wallet]): Wallet =
    w.getOrElse(throw new IllegalStateException("Invalid state"))

  private def fireEvent(subscribers: Vector[ActorRef])(event: Event): Unit =
    subscribers.foreach(_ ! event)

  override def receive: Receive = onMessage(None, Vector.empty, Vector.empty)

  private def onMessage(
      maybeWallet: Option[Wallet],
      requests: Vector[RequestRequest],
      subscribers: Vector[ActorRef]
  ): Receive = {
    case m @ GetBalanceRequest(_, walletId) if walletId == id =>
      log.debug(s"message = $m")
      sender() ! GetBalanceResponse(getWallet(maybeWallet).balance)

    case m @ AddSubscribers(_, walletId, s) if walletId == id =>
      log.debug(s"message = $m")
      context.become(onMessage(maybeWallet, requests, subscribers ++ s))

    case m @ CreateWalletRequest(_, walletId, createdAt) if walletId == id =>
      log.debug(s"message = $m")
      if (maybeWallet.isEmpty)
        sender() ! CreateWalletSucceeded
      else
        sender() ! CreateWalletFailed("Already created")
      fireEvent(subscribers)(WalletCreated(walletId, createdAt))
      context.become(onMessage(Some(Wallet(id, Balance(Money.zero))), requests, subscribers))

    case m @ DepositRequest(_, walletId, money, instant) if walletId == id =>
      log.debug(s"message = $m")
      val currentBalance = getWallet(maybeWallet).balance
      if (currentBalance.add(money) < Balance.zero)
        sender() ! DepositFailed("Can not trade because the balance after trading is less than 0")
      else
        sender() ! DepositSucceeded
      fireEvent(subscribers)(WalletDeposited(walletId, money, instant))
      context.become(
        onMessage(
          maybeWallet.map(_.withBalance(currentBalance.add(money))),
          requests,
          subscribers
        )
      )

    case m @ PayRequest(_, walletId, toWalletId, money, requestId, instant)
        if walletId == id && requestId.fold(true)(requests.map(_.requestId).contains) =>
      log.debug(s"message = $m")
      val currentBalance = getWallet(maybeWallet).balance
      if (currentBalance.sub(money) < Balance.zero)
        sender() ! PayFailed("Can not trade because the balance after trading is less than 0")
      else
        sender() ! PaySucceeded

      context.become(
        onMessage(
          maybeWallet.map(_.subBalance(money)),
          requests.filterNot(requestId.contains),
          subscribers
        )
      )
      fireEvent(subscribers)(WalletPayed(walletId, toWalletId, money, requestId, instant))

    case rr @ RequestRequest(_, requestId, walletId, toWalletId, money, instant) if walletId == id =>
      log.debug(s"message = $rr")
      if (requests.size > requestsLimit)
        sender() ! RequestFailed("Limit over")
      else
        sender() ! RequestSucceeded
      fireEvent(subscribers)(WalletRequested(requestId, walletId, toWalletId, money, instant))
      context.become(
        onMessage(
          maybeWallet,
          requests :+ rr,
          subscribers
        )
      )

    case m @ Shutdown(_, walletId) if walletId == id =>
      log.debug(s"message = $m")
      context.stop(self)
  }

}

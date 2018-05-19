package stockmonitoringbot.messengerservices

import info.mukel.telegrambot4s.methods.SendMessage
import stockmonitoringbot.datastorage.models._
import stockmonitoringbot.stockpriceservices.models.{CurrencyExchangeRateInfo, StockInfo}

import scala.concurrent.Future

/**
  * Created by amir.
  */
trait UserActorServiceComponent {
  val userActorService: UserActorService
}

trait UserActorService {

  def getUsersDailyNotifications(userId: Long): Future[Seq[DailyNotification]]
  def addDailyNotification(notification: DailyNotification): Future[DailyNotification]
  def deleteDailyNotification(notificationId: Long): Future[Unit]

  def getUsersTriggerNotifications(userId: Long): Future[Seq[TriggerNotification]]
  def addTriggerNotification(notification: TriggerNotification): Future[TriggerNotification]
  def deleteTriggerNotification(notificationId: Long): Future[Unit]

  def addPortfolio(portfolio: Portfolio): Future[Portfolio]
  def deletePortfolio(userId: Long, portfolioName: String): Future[Unit]
  def getUserPortfolios(userId: Long): Future[Seq[Portfolio]]
  def getPortfolio(userId: Long, portfolioName: String): Future[Portfolio]
  def addStockToPortfolio(userId: Long, portfolioName: String, stock: String, count: Double): Future[Unit]
  def deleteStockFromPortfolio(userId: Long, portfolioName: String, stock: String): Future[Unit]

  def getUserNotificationOnAsset(userId: Long, assetType: AssetType): Future[Option[DailyNotification]]

  def getUserTriggerNotificationOnAsset(userId: Long, assetType: AssetType): Future[Seq[TriggerNotification]]

  def getUser(userId: Long): Future[Option[User]]
  def setUser(user: User): Future[Unit]

  def getStockInfo(stock: String): Future[StockInfo]
  def cacheContains(stock: String): Boolean

  def getExchangeRate(from: String, to: String): Future[CurrencyExchangeRateInfo]

  def getPortfolioCurrentPrice(p: Portfolio): Future[BigDecimal]
  def getPortfolioStocksPrice(portfolio: Portfolio): Future[Map[String, BigDecimal]]

  def sendMessage(sendMessage: SendMessage): Unit
}

package stockmonitoringbot.datastorage

import stockmonitoringbot.datastorage.models._

import scala.concurrent.Future

/**
  * Created by amir.
  */
trait UserDataStorageComponent {
  val userDataStorage: UserDataStorage
}

trait UserDataStorage {
  def getUsersDailyNotifications(userId: Long): Future[Seq[DailyNotification]]
  def addDailyNotification(notification: DailyNotification): Future[Unit]
  def deleteDailyNotification(notification: DailyNotification): Future[Unit]

  def getUsersTriggerNotifications(userId: Long): Future[Seq[TriggerNotification]]
  def addTriggerNotification(notification: TriggerNotification): Future[Unit]
  def deleteTriggerNotification(notification: TriggerNotification): Future[Unit]

  def getAllTriggerNotifications: Future[Iterable[TriggerNotification]]

  def addPortfolio(portfolio: Portfolio): Future[Unit]
  def deletePortfolio(userId: Long, portfolioName: String): Future[Unit]
  def getUserPortfolios(userId: Long): Future[Seq[Portfolio]]
  def getPortfolio(userId: Long, portfolioName: String): Future[Portfolio]
  def addStockToPortfolio(userId: Long, portfolioName: String, stock: String, count: Double): Future[Unit]
  def deleteStockFromPortfolio(userId: Long, portfolioName: String, stock: String): Future[Unit]

  def getUserPortfolioNotification(userId: Long, portfolioName: String): Future[Option[PortfolioDailyNotification]]
  def deleteUserPortfolioNotification(userId: Long, portfolioName: String): Future[Unit]
  def setUserPortfolioNotification(userId: Long, portfolioName: String, notification: PortfolioDailyNotification): Future[Unit]

  def getUserPortfolioTriggerNotification(userId: Long, portfolioName: String): Future[Seq[PortfolioTriggerNotification]]
}

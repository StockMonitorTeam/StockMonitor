package stockmonitoringbot.datastorage

import stockmonitoringbot.datastorage.models.{DailyNotification, Portfolio, TriggerNotification}

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
  def getUsersPortfolios(userId: Long): Future[Seq[Portfolio]]
  def getPortfolio(userId: Long, portfolioName: String): Future[Portfolio]
  def addStockToPortfolio(userId: Long, portfolioName: String, stock: String, count: Double): Future[Unit]
  def deleteStockFromPortfolio(userId: Long, portfolioName: String, stock: String): Future[Unit]

}

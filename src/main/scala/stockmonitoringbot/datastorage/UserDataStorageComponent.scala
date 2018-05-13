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
  def deleteDailyNotification(notificationId: Long): Future[Unit]

  def getUsersTriggerNotifications(userId: Long): Future[Seq[TriggerNotification]]
  def addTriggerNotification(notification: TriggerNotification): Future[Unit]
  def deleteTriggerNotification(notificationId: Long): Future[Unit]

  def getAllTriggerNotifications: Future[Iterable[TriggerNotification]]

  def addPortfolio(portfolio: Portfolio): Future[Unit]
  def deletePortfolio(userId: Long, portfolioName: String): Future[Unit]
  def getUserPortfolios(userId: Long): Future[Seq[Portfolio]]
  def getPortfolio(userId: Long, portfolioName: String): Future[Portfolio]
  def addStockToPortfolio(userId: Long, portfolioName: String, stock: String, count: Double): Future[Unit]
  def deleteStockFromPortfolio(userId: Long, portfolioName: String, stock: String): Future[Unit]

  //actions with daily notifications on specified asset
  def getUserNotificationOnAsset(userId: Long, assetType: AssetType): Future[Option[DailyNotification]]

  //actions with trigger notifications on specified asset
  def getUserTriggerNotificationOnAsset(userId: Long, assetType: AssetType): Future[Seq[TriggerNotification]]

  def getUser(userId: Long): Future[Option[User]]
  def setUser(user: User): Future[Unit]

  def initDB(): Future[Unit]
}

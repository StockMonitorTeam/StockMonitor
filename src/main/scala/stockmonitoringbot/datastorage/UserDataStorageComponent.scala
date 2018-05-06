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

  //actions with daily notifications on specified asset
  def getUserNotification(userId: Long, assetType: AssetType): Future[Option[DailyNotification]]
  def deleteUserNotification(userId: Long, assetType: AssetType): Future[Unit]
  def setUserNotification(userId: Long, assetType: AssetType, notification: DailyNotification): Future[Unit]

  //actions with triger notifications on specified asset
  def getUserTriggerNotification(userId: Long, assetType: AssetType): Future[Seq[TriggerNotification]]

  def getUser(userId: Long): Future[Option[User]]
  def setUser(user: User): Future[Unit]
}

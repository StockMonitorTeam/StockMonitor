package stockmonitoringbot.datastorage.postgresdb

import slick.jdbc.PostgresProfile.api._
import slick.jdbc.meta.MTable
import stockmonitoringbot.datastorage.models._

import scala.concurrent.ExecutionContext

/**
  * Created by amir.
  */
object Queries extends Schema {

  val tables = Seq(users, dailyNotifications, triggerNotifications, portfolios, stocksInPortfolios)
  def ddl(implicit ec: ExecutionContext): DBIO[Seq[Unit]] = MTable.getTables.flatMap { existingTables =>
    val names = existingTables.map(mt => mt.name.name)
    val tableCreations = tables
      .filter(table => !names.contains(table.baseTableRow.tableName))
      .map(table => table.schema.create)
    DBIO.sequence(tableCreations)
  }

  import Schema._

  def getUsersDailyNotificationsSQL(userId: Long): DBIO[Seq[DailyNotification]] =
    dailyNotifications.filter(_.userId === userId).result
  def addDailyNotificationSQL(notification: DailyNotification): DBIO[Int] =
    dailyNotifications += notification
  def deleteDailyNotificationSQL(userId: Long, assetType: AssetType, assetName: String): DBIO[Int] =
    dailyNotifications.filter(x => x.userId === userId &&
      x.assetType === assetType && x.assetName === assetName).delete

  def getUsersTriggerNotificationsSQL(userId: Long): DBIO[Seq[TriggerNotification]] =
    triggerNotifications.filter(_.userId === userId).result
  def addTriggerNotificationSQL(notification: TriggerNotification): DBIO[Int] =
    triggerNotifications += notification
  def deleteTriggerNotificationSQL(userId: Long, assetType: AssetType, assetName: String,
                                   bound: BigDecimal, boundType: TriggerNotificationType): DBIO[Int] =
    triggerNotifications.filter(x => x.userId === userId && x.assetType === assetType
      && x.assetName === assetName && x.bound === bound && x.boundType === boundType).delete

  def getAllTriggerNotificationsSQL: DBIO[Seq[TriggerNotification]] = triggerNotifications.result

  def addPortfolioSQL(userId: Long, name: String, currency: Currency): DBIO[Int] =
    portfolios += ((0, userId, name, currency))
  def deletePortfolioSQL(userId: Long, name: String): DBIO[Int] =
    portfolios.filter(x => x.userId === userId && x.name === name).delete
  def getUserPortfoliosSQL(userId: Long): DBIO[Seq[(Long, Long, String, Currency)]] =
    portfolios.filter(_.userId === userId).result

  def getPortfolioSQL(userId: Long, name: String): DBIO[Seq[(Long, Long, String, Currency)]] =
    portfolios.filter(x => x.userId === userId && x.name === name).result
  def getPortfolioStocksSQL(portfolioId: Long): DBIO[Seq[(Long, String, Double)]] =
    stocksInPortfolios.filter(_.portfolioId === portfolioId).result

  def addStockToPortfolioSQL(portfolioId: Long, stock: String, count: Double): DBIO[Int] =
    stocksInPortfolios += ((portfolioId, stock, count))
  def deleteStockFromPortfolioSQL(portfolioId: Long, stock: String): DBIO[Int] =
    stocksInPortfolios.filter(x => x.portfolioId === portfolioId && x.stockName === stock).delete

  def getUserDailyNotificationOnAssetSQL(userId: Long, assetType: AssetType, name: String): DBIO[Seq[DailyNotification]] =
    dailyNotifications.filter(x => x.userId === userId && x.assetType === assetType && x.assetName === name).result
  def getUserTriggerNotificationsOnAssetSQL(userId: Long, assetType: AssetType, name: String): DBIO[Seq[TriggerNotification]] =
    triggerNotifications.filter(x => x.userId === userId && x.assetType === assetType && x.assetName === name).result

  def getUserSQL(userId: Long): DBIO[Seq[User]] = users.filter(_.id === userId).result
  def setUserSQL(user: User): DBIO[Int] = users.insertOrUpdate(user)

}

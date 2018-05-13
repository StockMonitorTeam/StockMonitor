package stockmonitoringbot.datastorage.postgresdb

import slick.jdbc.PostgresProfile.api._
import slick.jdbc.meta.MTable
import stockmonitoringbot.datastorage.models._
import stockmonitoringbot.datastorage.postgresdb.Queries._

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
  import CompiledQueries._

  def getUsersDailyNotificationsSQL(userId: Long): DBIO[Seq[DailyNotification]] =
    getUsersDailyNotificationsCompiledSQL(userId).result
  def addDailyNotificationSQL(notification: DailyNotification): DBIO[Int] =
    dailyNotifications += notification
  def deleteDailyNotificationSQL(id: Long): DBIO[Int] =
    deleteDailyNotificationCompiledSQL(id).delete

  def getUsersTriggerNotificationsSQL(userId: Long): DBIO[Seq[TriggerNotification]] =
    getUsersTriggerNotificationsCompiledSQL(userId).result
  def addTriggerNotificationSQL(notification: TriggerNotification): DBIO[Int] =
    triggerNotifications += notification
  def deleteTriggerNotificationSQL(id: Long): DBIO[Int] =
    deleteTriggerNotificationCompiledSQL(id).delete

  def getAllTriggerNotificationsSQL: DBIO[Seq[TriggerNotification]] = triggerNotifications.result

  def addPortfolioSQL(portfolio: Portfolio): DBIO[Int] =
    portfolios += portfolio
  def deletePortfolioSQL(userId: Long, name: String): DBIO[Int] =
    deletePortfolioCompiledSQL((userId, name)).delete
  def getUserPortfoliosSQL(userId: Long): DBIO[Seq[Portfolio]] =
    getUserPortfoliosCompiledSQL(userId).result

  def getPortfolioSQL(userId: Long, name: String): DBIO[Seq[Portfolio]] =
    getPortfolioCompiledSQL((userId, name)).result
  def getPortfolioStocksSQL(portfolioId: Long): DBIO[Seq[(Long, String, Double)]] =
    getPortfolioStocksCompiledSQL(portfolioId).result

  def addStockToPortfolioSQL(portfolioId: Long, stock: String, count: Double): DBIO[Int] =
    stocksInPortfolios += ((portfolioId, stock, count))
  def deleteStockFromPortfolioSQL(portfolioId: Long, stock: String): DBIO[Int] =
    deleteStockFromPortfolioCompiledSQL((portfolioId, stock)).delete

  def getUserDailyNotificationOnAssetSQL(userId: Long, assetType: AssetType, name: String): DBIO[Seq[DailyNotification]] =
    getUserDailyNotificationOnAssetCompiledSQL((userId, assetType, name)).result
  def getUserTriggerNotificationsOnAssetSQL(userId: Long, assetType: AssetType, name: String): DBIO[Seq[TriggerNotification]] =
    getUserTriggerNotificationsOnAssetCompiledSQL((userId, assetType, name)).result

  def getUserSQL(userId: Long): DBIO[Seq[User]] = getUserCompiledSQL(userId).result
  def setUserSQL(user: User): DBIO[Int] = users.insertOrUpdate(user)

}

object CompiledQueries {

  import Schema._

  val getUsersDailyNotificationsCompiledSQL = Compiled {
    userId: Rep[Long] => dailyNotifications.filter(_.userId === userId)
  }
  val deleteDailyNotificationCompiledSQL = Compiled {
    (id: Rep[Long]) =>
      dailyNotifications.filter(_.dailyNotId === id)
  }
  val getUsersTriggerNotificationsCompiledSQL = Compiled {
    userId: Rep[Long] => triggerNotifications.filter(_.userId === userId)
  }
  val deleteTriggerNotificationCompiledSQL = Compiled {
    (id: Rep[Long]) =>
      triggerNotifications.filter(_.triggerNotId === id)
  }

  val deletePortfolioCompiledSQL = Compiled {
    (userId: Rep[Long], name: Rep[String]) => portfolios.filter(x => x.userId === userId && x.name === name)
  }
  val getUserPortfoliosCompiledSQL = Compiled {
    userId: Rep[Long] => portfolios.filter(_.userId === userId)
  }
  val getPortfolioCompiledSQL = Compiled {
    (userId: Rep[Long], name: Rep[String]) => portfolios.filter(x => x.userId === userId && x.name === name)
  }
  val getPortfolioStocksCompiledSQL = Compiled {
    (portfolioId: Rep[Long]) => stocksInPortfolios.filter(_.portfolioId === portfolioId)
  }

  val deleteStockFromPortfolioCompiledSQL = Compiled {
    (portfolioId: Rep[Long], stock: Rep[String]) =>
      stocksInPortfolios.filter(x => x.portfolioId === portfolioId && x.stockName === stock)
  }

  val getUserDailyNotificationOnAssetCompiledSQL = Compiled {
    (userId: Rep[Long], assetType: Rep[AssetType], name: Rep[String]) =>
      dailyNotifications.filter(x => x.userId === userId && x.assetType === assetType && x.assetName === name)
  }
  val getUserTriggerNotificationsOnAssetCompiledSQL = Compiled {
    (userId: Rep[Long], assetType: Rep[AssetType], name: Rep[String]) =>
      triggerNotifications.filter(x => x.userId === userId && x.assetType === assetType && x.assetName === name)
  }

  val getUserCompiledSQL = Compiled {
    (userId: Rep[Long]) => users.filter(_.id === userId)
  }
}

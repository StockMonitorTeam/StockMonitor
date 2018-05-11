package stockmonitoringbot.datastorage.postgresdb

import java.time.{LocalTime, ZoneId}

import slick.ast.BaseTypedType
import slick.jdbc.JdbcType

import slick.jdbc.PostgresProfile.api._
import stockmonitoringbot.datastorage.models._

/**
  * Created by amir.
  */
trait Schema {

  val users = TableQuery[Users]
  val dailyNotifications = TableQuery[DailyNotifications]
  val triggerNotifications = TableQuery[TriggerNotifications]
  val portfolios = TableQuery[Portfolios]
  val stocksInPortfolios = TableQuery[StocksInPortfolios]

  import Schema._

  class Users(tag: Tag) extends Table[User](tag, "USERS") {
    def id = column[Long]("USER_ID", O.PrimaryKey)
    def zoneId = column[ZoneId]("TIME_ZONE")

    def * = (id, zoneId) <> (User.tupled, User.unapply)
  }

  class DailyNotifications(tag: Tag) extends Table[DailyNotification](tag, "DAILY_NOTIFICATIONS") {
    def userId = column[Long]("USER_ID")
    def assetType = column[AssetType]("TYPE")
    def assetName = column[String]("ASSET_NAME")
    def time = column[LocalTime]("ALERT_TIME")

    def * = (userId, assetType, assetName, time) <>
      (mapRowToDailyNotification, unapplyDailyNotification)
    def userFK = foreignKey("USER_FK", userId, users)(_.id, onDelete = ForeignKeyAction.Cascade)
    def PK = primaryKey("PK", (userId, assetType, assetName))
  }

  class TriggerNotifications(tag: Tag) extends Table[TriggerNotification](tag, "TRIGGER_NOTIFICATIONS") {
    def userId = column[Long]("USER_ID")
    def assetType = column[AssetType]("TYPE")
    def assetName = column[String]("ASSET_NAME")
    def bound = column[BigDecimal]("BOUND")
    def boundType = column[TriggerNotificationType]("BOUND_TYPE")

    def * = (userId, assetType, assetName, bound, boundType) <>
      (mapRowToTriggerNotification, unapplyTriggerNotification)
    def userFK = foreignKey("USER_FK", userId, users)(_.id, onDelete = ForeignKeyAction.Cascade)
    def PK = primaryKey("PK", (userId, assetType, assetName, bound, boundType))
  }

  class Portfolios(tag: Tag) extends Table[(Long, Long, String, Currency)](tag, "PORTFOLIOS") {
    def portfolioId = column[Long]("PORTFOLIO_ID", O.PrimaryKey, O.AutoInc)
    def userId = column[Long]("USER_ID")
    def name = column[String]("NAME")
    def currency = column[Currency]("CURRENCY")

    def * = (portfolioId, userId, name, currency)
    def userFK = foreignKey("USER_FK", userId, users)(_.id, onDelete = ForeignKeyAction.Cascade)
  }

  class StocksInPortfolios(tag: Tag) extends Table[(Long, String, Double)](tag, "STOCKS_IN_PORTFOLIOS") {
    def portfolioId = column[Long]("PORTFOLIO_ID")
    def stockName = column[String]("STOCK_NAME")
    def amount = column[Double]("AMOUNT")

    def * = (portfolioId, stockName, amount)
    def portfolioFK = foreignKey("PORTFOLIO_FK", portfolioId, portfolios)(_.portfolioId, onDelete = ForeignKeyAction.Cascade)
    def PK = primaryKey("PK", (portfolioId, stockName))
  }

}

object Schema {
  sealed trait AssetType
  case object Stock extends AssetType
  case object ExchangeRate extends AssetType
  case object Portfolio extends AssetType

  implicit val assetTypeColumnType: JdbcType[AssetType] with BaseTypedType[AssetType] = MappedColumnType.base[AssetType, Int]({
    case Stock => 0
    case ExchangeRate => 1
    case Portfolio => 2
  }, {
    case 0 => Stock
    case 1 => ExchangeRate
    case 2 => Portfolio
  })
  implicit val boundTypeColumnType: JdbcType[TriggerNotificationType] with BaseTypedType[TriggerNotificationType] = MappedColumnType.base[TriggerNotificationType, Int]({
    case Raise => 0
    case Fall => 1
    case Both => 2
  }, {
    case 0 => Raise
    case 1 => Fall
    case 2 => Both
  })
  implicit val currencyColumnType: JdbcType[Currency] with BaseTypedType[Currency] = MappedColumnType.base[Currency, Int]({
    case USD => 0
    case EUR => 1
    case RUB => 2
  }, {
    case 0 => USD
    case 1 => EUR
    case 2 => RUB
  })

  implicit val localTimeColumnType: JdbcType[LocalTime] with BaseTypedType[LocalTime] = MappedColumnType.base[LocalTime, String](_.toString, LocalTime.parse)
  implicit val bigDecimalColumnType: JdbcType[BigDecimal] with BaseTypedType[BigDecimal] = MappedColumnType.base[BigDecimal, String](_.toString, BigDecimal.apply)
  implicit val timeZoneColumnType: JdbcType[ZoneId] with BaseTypedType[ZoneId] = MappedColumnType.base[ZoneId, String](_.toString, ZoneId.of)

  def getNameAndAssetType(n: Notification): (String, AssetType) = n match {
    case x: StockNotification => (x.stock, Stock)
    case x: ExchangeRateNotification => (s"${x.exchangePair._1}/${x.exchangePair._2}", ExchangeRate)
    case x: PortfolioNotification => (x.portfolioName, Portfolio)
  }

  private def mapRowToTriggerNotification(row: (Long, Schema.AssetType, String, BigDecimal, TriggerNotificationType)): TriggerNotification =
    row._2 match {
      case Stock => StockTriggerNotification(row._1, row._3, row._4, row._5)
      case ExchangeRate =>
        val curr = row._3.split("/")
        ExchangeRateTriggerNotification(row._1, (curr(0), curr(1)), row._4, row._5)
      case Portfolio => PortfolioTriggerNotification(row._1, row._3, row._4, row._5)
    }

  private def unapplyTriggerNotification(n: TriggerNotification) = {
    val (name, t) = getNameAndAssetType(n)
    Some((n.ownerId, t, name, n.boundPrice, n.notificationType))
  }

  private def mapRowToDailyNotification(row: (Long, Schema.AssetType, String, LocalTime)): DailyNotification =
    row._2 match {
      case Stock => StockDailyNotification(row._1, row._3, row._4)
      case ExchangeRate =>
        val curr = row._3.split("/")
        ExchangeRateDailyNotification(row._1, (curr(0), curr(1)), row._4)
      case Portfolio => PortfolioDailyNotification(row._1, row._3, row._4)
    }

  private def unapplyDailyNotification(n: DailyNotification) = {
    val (name, t) = getNameAndAssetType(n)
    Some((n.ownerId, t, name, n.time))
  }
}

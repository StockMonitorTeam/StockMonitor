package stockmonitoringbot.datastorage.postgresdb

import java.sql.Timestamp
import java.time.{LocalTime, ZoneId}

import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.jdbc.PostgresProfile.api._
import stockmonitoringbot.datastorage.models
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
  val usersHistory = TableQuery[UsersHistory]

  import Schema._

  class Users(tag: Tag) extends Table[User](tag, "USERS") {
    def id = column[Long]("USER_ID", O.PrimaryKey)
    def zoneId = column[ZoneId]("TIME_ZONE")

    def * = (id, zoneId) <> (User.tupled, User.unapply)
  }

  class DailyNotifications(tag: Tag) extends Table[DailyNotification](tag, "DAILY_NOTIFICATIONS") {
    def dailyNotId = column[Long]("DAILY_NOT_ID", O.PrimaryKey, O.AutoInc)
    def userId = column[Long]("USER_ID")
    def assetType = column[AssetType]("TYPE")
    def assetName = column[String]("ASSET_NAME")
    def time = column[LocalTime]("ALERT_TIME")

    def * = (dailyNotId, userId, assetType, assetName, time) <>
      (mapRowToDailyNotification, unapplyDailyNotification)
    def userFK = foreignKey("USER_FK", userId, users)(_.id, onDelete = ForeignKeyAction.Cascade)
    def uniqueForAsset = index("ONE_NOT_FOR_ASSET", (userId, assetType, assetName), unique = true)
  }

  class TriggerNotifications(tag: Tag) extends Table[TriggerNotification](tag, "TRIGGER_NOTIFICATIONS") {
    def triggerNotId = column[Long]("TRIGGER_NOT_ID", O.PrimaryKey, O.AutoInc)
    def userId = column[Long]("USER_ID")
    def assetType = column[AssetType]("TYPE")
    def assetName = column[String]("ASSET_NAME")
    def bound = column[BigDecimal]("BOUND")
    def boundType = column[TriggerNotificationType]("BOUND_TYPE")

    def * = (triggerNotId, userId, assetType, assetName, bound, boundType) <>
      (mapRowToTriggerNotification, unapplyTriggerNotification)
    def userFK = foreignKey("USER_FK", userId, users)(_.id, onDelete = ForeignKeyAction.Cascade)
  }

  class Portfolios(tag: Tag) extends Table[Portfolio](tag, "PORTFOLIOS") {
    def portfolioId = column[Long]("PORTFOLIO_ID", O.PrimaryKey, O.AutoInc)
    def userId = column[Long]("USER_ID")
    def name = column[String]("NAME")
    def currency = column[Currency]("CURRENCY")

    def * = (portfolioId, userId, name, currency) <> (mapRowToPortfolio, unapplyPortfolio)
    def userFK = foreignKey("USER_FK", userId, users)(_.id, onDelete = ForeignKeyAction.Cascade)
  }

  class StocksInPortfolios(tag: Tag) extends Table[(Long, String, Double)](tag, "STOCKS_IN_PORTFOLIOS") {
    def portfolioId = column[Long]("PORTFOLIO_ID")
    def stockName = column[String]("STOCK_NAME")
    def amount = column[Double]("AMOUNT")

    def * = (portfolioId, stockName, amount)
    def portfolioFK = foreignKey("PORTFOLIO_FK", portfolioId, portfolios)(_.portfolioId, onDelete = ForeignKeyAction.Cascade)
    def PK = primaryKey("STOCKS_PK", (portfolioId, stockName))
  }

  class UsersHistory(tag: Tag) extends Table[UserQuery](tag, "USERS_HISTORY") {
    def queryId = column[Long]("QUERY_ID", O.PrimaryKey, O.AutoInc)
    def userId = column[Long]("USER_ID")
    def assetType = column[AssetType]("ASSET_TYPE")
    def assetName = column[String]("ASSET_NAME")
    def timestamp = column[Timestamp]("CREATED")

    def * = (queryId, userId, assetType, assetName, timestamp) <> (mapRowToUserQuery, unapplyUserQuery)
    def userFK = foreignKey("USER_FK", userId, users)(_.id, onDelete = ForeignKeyAction.Cascade)
  }

}

object Schema {
  sealed trait AssetType
  case object Stock extends AssetType
  case object ExchangeRate extends AssetType
  case object Portfolio extends AssetType

  implicit val assetTypeColumnType: JdbcType[AssetType] with BaseTypedType[AssetType] = MappedColumnType.base[AssetType, String](
    {
      case Stock => "Stock"
      case ExchangeRate => "ExchangeRate"
      case Portfolio => "Portfolio"
    }, {
      case "Stock" => Stock
      case "ExchangeRate" => ExchangeRate
      case "Portfolio" => Portfolio
    })
  implicit val boundTypeColumnType: JdbcType[TriggerNotificationType] with BaseTypedType[TriggerNotificationType] =
    MappedColumnType.base[TriggerNotificationType, String]({
      case Raise => "Raise"
      case Fall => "Fall"
      case Both => "Both"
    }, {
      case "Raise" => Raise
      case "Fall" => Fall
      case "Both" => Both
    })
  implicit val currencyColumnType: JdbcType[Currency] with BaseTypedType[Currency] = MappedColumnType.base[Currency, String]({
    case USD => "USD"
    case EUR => "EUR"
    case RUB => "RUB"
  }, {
    case "USD" => USD
    case "EUR" => EUR
    case "RUB" => RUB
  })

  implicit val localTimeColumnType: JdbcType[LocalTime] with BaseTypedType[LocalTime] = MappedColumnType.base[LocalTime, String](_.toString, LocalTime.parse)
  implicit val bigDecimalColumnType: JdbcType[BigDecimal] with BaseTypedType[BigDecimal] = MappedColumnType.base[BigDecimal, String](_.toString, BigDecimal.apply)
  implicit val timeZoneColumnType: JdbcType[ZoneId] with BaseTypedType[ZoneId] = MappedColumnType.base[ZoneId, String](_.toString, ZoneId.of)

  def getNameAndAssetType(n: Notification): (String, AssetType) = n match {
    case x: StockNotification => (x.stock, Stock)
    case x: ExchangeRateNotification => (s"${x.exchangePair._1}/${x.exchangePair._2}", ExchangeRate)
    case x: PortfolioNotification => (x.portfolioName, Portfolio)
  }

  private def mapRowToTriggerNotification(row: (Long, Long, Schema.AssetType, String, BigDecimal, TriggerNotificationType)): TriggerNotification = {
    val (notificationId, userId, assetType, assetName, bound, boundType) = row
    assetType match {
      case Stock => StockTriggerNotification(notificationId, userId, assetName, bound, boundType)
      case ExchangeRate =>
        val curr = assetName.split("/")
        ExchangeRateTriggerNotification(notificationId, userId, (curr(0), curr(1)), bound, boundType)
      case Portfolio => PortfolioTriggerNotification(notificationId, userId, assetName, bound, boundType)
    }
  }

  private def unapplyTriggerNotification(n: TriggerNotification) = {
    val (name, t) = getNameAndAssetType(n)
    Some((n.id, n.ownerId, t, name, n.boundPrice, n.notificationType))
  }

  private def mapRowToDailyNotification(row: (Long, Long, Schema.AssetType, String, LocalTime)): DailyNotification = {
    val (notificationId, userId, assetType, assetName, time) = row
    assetType match {
      case Stock => StockDailyNotification(notificationId, userId, assetName, time)
      case ExchangeRate =>
        val curr = assetName.split("/")
        ExchangeRateDailyNotification(notificationId, userId, (curr(0), curr(1)), time)
      case Portfolio => PortfolioDailyNotification(notificationId, userId, assetName, time)
    }
  }

  private def unapplyDailyNotification(n: DailyNotification) = {
    val (name, t) = getNameAndAssetType(n)
    Some((n.id, n.ownerId, t, name, n.time))
  }

  private def mapRowToPortfolio(row: (Long, Long, String, Currency)): Portfolio = {
    val (portfolioId, userId, name, currency) = row
    models.Portfolio(portfolioId, userId, name, currency, Map.empty)
  }

  private def unapplyPortfolio(p: Portfolio) = {
    Some((p.portfolioId, p.userId, p.name, p.currency))
  }

  private def mapRowToUserQuery(row: (Long, Long, Schema.AssetType, String, Timestamp)): UserQuery = {
    val (_, userId, assetType, assetName, timestamp) = row
    assetType match {
      case Stock => UserQuery(userId, StockAsset(assetName), timestamp)
      case ExchangeRate =>
        val curr = assetName.split("/")
        UserQuery(userId, ExchangeRateAsset(curr(0), curr(1)), timestamp)
      case Portfolio => UserQuery(userId, PortfolioAsset(assetName), timestamp)
    }
  }

  private def unapplyUserQuery(q: UserQuery) = {
    val (t, name) = q.assetType match {
      case StockAsset(stock) => (Schema.Stock, stock)
      case ExchangeRateAsset(from, to) => (Schema.ExchangeRate, s"$from/$to")
      case PortfolioAsset(portfolio) => (Schema.Portfolio, portfolio)
    }
    Some((0L, q.userId, t, name, q.time))
  }

}

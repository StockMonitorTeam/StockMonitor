package stockmonitoringbot.datastorage.postgresdb

import java.time.{LocalTime, ZoneId}

import slick.lifted.{TableQuery, Tag}
import slick.jdbc.PostgresProfile.api._

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

  class Users(tag: Tag) extends Table[(Long, ZoneId)](tag, "USERS") {
    def id = column[Long]("USER_ID", O.PrimaryKey)
    def zoneId = column[ZoneId]("TIME_ZONE")

    def * = (id, zoneId)
  }

  class DailyNotifications(tag: Tag) extends Table[(Long, AssetType, String, LocalTime)](tag, "DAILY_NOTIFICATIONS") {
    def userId = column[Long]("USER_ID")
    def assetType = column[AssetType]("TYPE")
    def assetName = column[String]("ASSET_NAME")
    def time = column[LocalTime]("ALERT_TIME")

    def * = (userId, assetType, assetName, time)
    def userFK = foreignKey("USER_FK", userId, users)(_.id, onDelete = ForeignKeyAction.Cascade)
    def PK = primaryKey("PK", (userId, assetType, assetName))
  }

  class TriggerNotifications(tag: Tag) extends Table[(Long, AssetType, String, BigDecimal, BoundType)](tag, "TRIGGER_NOTIFICATIONS") {
    def userId = column[Long]("USER_ID")
    def assetType = column[AssetType]("TYPE")
    def assetName = column[String]("ASSET_NAME")
    def bound = column[BigDecimal]("BOUND")
    def boundType = column[BoundType]("BOUND_TYPE")

    def * = (userId, assetType, assetName, bound, boundType)
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

  sealed trait Currency
  case object USD extends Currency
  case object EUR extends Currency
  case object RUB extends Currency

  sealed trait BoundType
  case object Raise extends BoundType
  case object Fall extends BoundType
  case object Both extends BoundType

  implicit val assetTypeColumnType = MappedColumnType.base[AssetType, Int]({
    case Stock => 0
    case ExchangeRate => 1
    case Portfolio => 2
  }, {
    case 0 => Stock
    case 1 => ExchangeRate
    case 2 => Portfolio
  })
  implicit val boundTypeColumnType = MappedColumnType.base[BoundType, Int]({
    case Raise => 0
    case Fall => 1
    case Both => 2
  }, {
    case 0 => Raise
    case 1 => Fall
    case 2 => Both
  })
  implicit val currencyColumnType = MappedColumnType.base[Currency, Int]({
    case USD => 0
    case EUR => 1
    case RUB => 2
  }, {
    case 0 => USD
    case 1 => EUR
    case 2 => RUB
  })
  implicit val localTimeColumnType = MappedColumnType.base[LocalTime, String](_.toString, LocalTime.parse)
  implicit val bigDecimalColumnType = MappedColumnType.base[BigDecimal, String](_.toString, BigDecimal.apply)
  implicit val timeZoneColumnType = MappedColumnType.base[ZoneId, String](_.toString, ZoneId.of)

}

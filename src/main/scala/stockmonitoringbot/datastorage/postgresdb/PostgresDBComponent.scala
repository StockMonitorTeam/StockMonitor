package stockmonitoringbot.datastorage.postgresdb

import java.time.LocalTime

import com.typesafe.scalalogging.Logger
import stockmonitoringbot.ExecutionContextComponent
import stockmonitoringbot.datastorage.postgresdb.Schema.{Both, BoundType, ExchangeRate, Fall, Portfolio, Raise, Stock}
import stockmonitoringbot.datastorage.postgresdb.exceptions.{ElementAlreadyExistsException, NoSuchPortfolioException}
import stockmonitoringbot.datastorage.models._
import stockmonitoringbot.datastorage.models
import stockmonitoringbot.datastorage.{UserDataStorage, UserDataStorageComponent}

import scala.concurrent.Future
import slick.jdbc.PostgresProfile.api._

import scala.util.{Failure, Success}

/**
  * Created by amir.
  */
trait PostgresDBComponent extends UserDataStorageComponent {
  this: PostgresDBConnectionComponent
    with ExecutionContextComponent =>

  override lazy val userDataStorage: UserDataStorage = new PostgresDB

  class PostgresDB extends UserDataStorage {

    import Queries._
    import PostgresDB._

    val logger: Logger = Logger(getClass)

    override def initDB(): Future[Unit] = {
      val initFut = DBConnection.run(ddl)
      initFut.onComplete {
        case Success(x) =>
          logger.info(s"Created ${x.size} tables")
        case Failure(e) =>
          logger.error("Can't create tables", e)
      }
      initFut.map(_ => ())
    }

    override def getUsersDailyNotifications(userId: Long): Future[Seq[DailyNotification]] =
      DBConnection.run(getUsersDailyNotificationsSQL(userId)).map(_.map(mapRawToDailyNotification))

    override def addDailyNotification(n: DailyNotification): Future[Unit] = {
      val (name, assetType) = getNameAndAssetType(n)
      DBConnection.run(addDailyNotificationSQL(n.ownerId, assetType, name, n.time)).flatMap {
        case 0 => Future.failed(new ElementAlreadyExistsException)
        case 1 => Future.successful(())
        case _ => Future.failed(new IllegalStateException())
      }
    }
    override def deleteDailyNotification(n: DailyNotification): Future[Unit] = {
      val (name, assetType) = getNameAndAssetType(n)
      DBConnection.run(deleteDailyNotificationSQL(n.ownerId, assetType, name)).flatMap {
        case 0 => Future.failed(new NoSuchElementException)
        case 1 => Future.successful(())
        case _ => Future.failed(new IllegalStateException())
      }
    }

    override def getUsersTriggerNotifications(userId: Long): Future[Seq[TriggerNotification]] =
      DBConnection.run(getUsersTriggerNotificationsSQL(userId)).map(_.map(mapRawToTriggerNotification))

    override def addTriggerNotification(n: TriggerNotification): Future[Unit] = {
      val (name, assetType) = getNameAndAssetType(n)
      DBConnection.run(addTriggerNotificationSQL(n.ownerId, assetType, name, n.boundPrice,
        triggerTypeToBoundType(n.notificationType))).flatMap {
        case 0 => Future.failed(new ElementAlreadyExistsException)
        case 1 => Future.successful(())
        case _ => Future.failed(new IllegalStateException())
      }
    }
    override def deleteTriggerNotification(n: TriggerNotification): Future[Unit] = {
      val (name, assetType) = getNameAndAssetType(n)
      DBConnection.run(deleteTriggerNotificationSQL(n.ownerId, assetType, name, n.boundPrice,
        triggerTypeToBoundType(n.notificationType))).flatMap {
        case 0 => Future.failed(new NoSuchElementException)
        case 1 => Future.successful(())
        case _ => Future.failed(new IllegalStateException())
      }
    }

    override def getAllTriggerNotifications: Future[Iterable[TriggerNotification]] =
      DBConnection.run(getAllTriggerNotificationsSQL).map(_.map(mapRawToTriggerNotification))
    override def addPortfolio(p: Portfolio): Future[Unit] =
      DBConnection.run(addPortfolioSQL(p.userId, p.name, currencyToCurrency(p.currency))).flatMap {
        case 0 => Future.failed(new ElementAlreadyExistsException)
        case 1 => Future.successful(())
        case _ => Future.failed(new IllegalStateException())
      }

    override def deletePortfolio(userId: Long, portfolioName: String): Future[Unit] =
      DBConnection.run(deletePortfolioSQL(userId, portfolioName)).flatMap {
        case 0 => Future.failed(new NoSuchElementException)
        case 1 => Future.successful(())
        case _ => Future.failed(new IllegalStateException())
      }
    override def getUserPortfolios(userId: Long): Future[Seq[Portfolio]] = {
      val request = getUserPortfoliosSQL(userId).flatMap { portfolios =>
        DBIO.sequence(portfolios.map { raw =>
          getPortfolioStocksSQL(raw._1).map(stocks =>
            models.Portfolio(raw._2, raw._3, currencyToCurrency(raw._4), stocks.map(x => (x._2, x._3)).toMap))
        })
      }
      DBConnection.run(request)
    }
    override def getPortfolio(userId: Long, portfolioName: String): Future[Portfolio] = {
      val request = getPortfolioSQL(userId, portfolioName).flatMap { x =>
        DBIO.sequenceOption(x.headOption.map { raw =>
          getPortfolioStocksSQL(raw._1).map(stocks =>
            models.Portfolio(raw._2, raw._3, currencyToCurrency(raw._4), stocks.map(x => (x._2, x._3)).toMap))
        })
      }
      DBConnection.run(request).map(_.get)
    }

    override def addStockToPortfolio(userId: Long, portfolioName: String, stock: String, count: Double): Future[Unit] = {
      val request = getPortfolioSQL(userId, portfolioName).flatMap { x =>
        DBIO.sequenceOption(x.headOption.map(raw => addStockToPortfolioSQL(raw._1, stock, count)))
      }
      DBConnection.run(request).flatMap {
        case None => Future.failed(new NoSuchPortfolioException)
        case Some(0) => Future.failed(new ElementAlreadyExistsException)
        case Some(1) => Future.successful(())
        case Some(_) => Future.failed(new IllegalStateException())
      }
    }

    override def deleteStockFromPortfolio(userId: Long, portfolioName: String, stock: String): Future[Unit] = {
      val request = getPortfolioSQL(userId, portfolioName).flatMap { x =>
        DBIO.sequenceOption(x.headOption.map(raw => deleteStockFromPortfolioSQL(raw._1, stock)))
      }
      DBConnection.run(request).flatMap {
        case None => Future.failed(new NoSuchPortfolioException)
        case Some(0) => Future.failed(new NoSuchElementException)
        case Some(1) => Future.successful(())
        case Some(_) => Future.failed(new IllegalStateException())
      }
    }
    override def getUserNotificationOnAsset(userId: Long, assetType: models.AssetType): Future[Option[DailyNotification]] = {
      val (t, name) = parseAssetType(assetType)
      DBConnection.run(getUserDailyNotificationOnAssetSQL(userId, t, name)).map(_.headOption.map(mapRawToDailyNotification))
    }
    override def getUserTriggerNotificationOnAsset(userId: Long, assetType: models.AssetType): Future[Seq[TriggerNotification]] = {
      val (t, name) = parseAssetType(assetType)
      DBConnection.run(getUserTriggerNotificationsOnAssetSQL(userId, t, name)).map(seq => seq.map(mapRawToTriggerNotification))
    }

    override def getUser(userId: Long): Future[Option[User]] =
      DBConnection.run(getUserSQL(userId)).map(_.headOption.map(row => User(row._1, row._2)))
    override def setUser(user: User): Future[Unit] =
      DBConnection.run(setUserSQL(user.id, user.timeZone)).flatMap {
        case 0 => Future.failed(new NoSuchElementException)
        case 1 => Future.successful(())
        case _ => Future.failed(new IllegalStateException())
      }

  }

  object PostgresDB {
    private def getNameAndAssetType(n: Notification) = n match {
      case x: StockNotification => (x.stock, Stock)
      case x: ExchangeRateNotification => (s"${x.exchangePair._1}/${x.exchangePair._2}", ExchangeRate)
      case x: PortfolioNotification => (x.portfolioName, Portfolio)
    }

    private def boundTypeToTriggerType(bt: BoundType) = bt match {
      case Raise => RaiseNotification
      case Fall => FallNotification
      case Both => BothNotification
    }

    private def triggerTypeToBoundType(bt: TriggerNotificationType) = bt match {
      case RaiseNotification => Raise
      case FallNotification => Fall
      case BothNotification => Both
    }

    private def currencyToCurrency(c: Currency) = c match {
      case USD => Schema.USD
      case EUR => Schema.EUR
      case RUB => Schema.RUB
    }

    private def currencyToCurrency(c: Schema.Currency) = c match {
      case Schema.USD => USD
      case Schema.EUR => EUR
      case Schema.RUB => RUB
    }

    private def parseAssetType(at: AssetType) = at match {
      case StockAsset(name) => (Stock, name)
      case ExchangeRateAsset(from, to) => (ExchangeRate, s"$from/$to")
      case PortfolioAsset(name) => (Portfolio, name)
    }

    private def mapRawToTriggerNotification(row: (Long, Schema.AssetType, String, BigDecimal, BoundType)) =
      row._2 match {
        case Stock => StockTriggerNotification(row._1, row._3, row._4, boundTypeToTriggerType(row._5))
        case ExchangeRate =>
          val curr = row._3.split("/")
          ExchangeRateTriggerNotification(row._1, (curr(0), curr(1)), row._4, boundTypeToTriggerType(row._5))
        case Portfolio => PortfolioTriggerNotification(row._1, row._3, row._4, boundTypeToTriggerType(row._5))
      }

    private def mapRawToDailyNotification(row: (Long, Schema.AssetType, String, LocalTime)) =
      row._2 match {
        case Stock => StockDailyNotification(row._1, row._3, row._4)
        case ExchangeRate =>
          val curr = row._3.split("/")
          ExchangeRateDailyNotification(row._1, (curr(0), curr(1)), row._4)
        case Portfolio => PortfolioDailyNotification(row._1, row._3, row._4)
      }

  }

}

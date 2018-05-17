package stockmonitoringbot.datastorage.postgresdb

import com.typesafe.scalalogging.Logger
import slick.jdbc.PostgresProfile.api._
import stockmonitoringbot.ExecutionContextComponent
import stockmonitoringbot.datastorage.models._
import stockmonitoringbot.datastorage.postgresdb.Schema.{ExchangeRate, Portfolio, Stock}
import stockmonitoringbot.datastorage.postgresdb.exceptions.{ElementAlreadyExistsException, NoSuchPortfolioException}
import stockmonitoringbot.datastorage.{UserDataStorage, UserDataStorageComponent, models}

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Created by amir.
  */
trait PostgresDBComponent extends UserDataStorageComponent {
  this: PostgresDBConnectionComponent
    with ExecutionContextComponent =>

  override lazy val userDataStorage: UserDataStorage = new PostgresDB

  class PostgresDB extends UserDataStorage {

    import PostgresDB._
    import Queries._

    val logger: Logger = Logger(getClass)

    override def initDB(): Future[Unit] = {
      val initFut = dbConnection.run(ddl)
      initFut.onComplete {
        case Success(x) =>
          logger.info(s"Created ${x.size} tables")
        case Failure(e) =>
          logger.error("Can't create tables", e)
      }
      initFut.map(_ => ())
    }

    override def getAllDailyNotifications: Future[Seq[DailyNotification]] =
      dbConnection.run(getAllDailyNotificationsSQL)

    override def getUsersDailyNotifications(userId: Long): Future[Seq[DailyNotification]] =
      dbConnection.run(getUsersDailyNotificationsSQL(userId))

    override def addDailyNotification(n: DailyNotification): Future[DailyNotification] = {
      dbConnection.run {
        addDailyNotificationSQL(n).map {
          case Some(id) => setId(n, id)
          case None => throw new ElementAlreadyExistsException
        }
      }
    }
    override def deleteDailyNotification(id: Long): Future[Unit] = {
      dbConnection.run {
        deleteDailyNotificationSQL(id).map {
          case 0 => throw new NoSuchElementException
          case 1 => ()
          case _ => throw new IllegalStateException()
        }
      }
    }

    override def getUsersTriggerNotifications(userId: Long): Future[Seq[TriggerNotification]] =
      dbConnection.run(getUsersTriggerNotificationsSQL(userId))

    override def addTriggerNotification(n: TriggerNotification): Future[TriggerNotification] = {
      dbConnection.run {
        addTriggerNotificationSQL(n).map {
          case Some(id) => setId(n, id)
          case None => throw new ElementAlreadyExistsException
        }
      }
    }
    override def deleteTriggerNotification(id: Long): Future[Unit] = {
      dbConnection.run {
        deleteTriggerNotificationSQL(id).map {
          case 0 => throw new NoSuchElementException
          case 1 => ()
          case _ => throw new IllegalStateException()
        }
      }
    }

    override def getAllTriggerNotifications: Future[Iterable[TriggerNotification]] =
      dbConnection.run(getAllTriggerNotificationsSQL)

    override def addPortfolio(p: Portfolio): Future[Portfolio] =
      dbConnection.run {
        addPortfolioSQL(p).map {
          case Some(id) => p.copy(portfolioId = id)
          case None => throw new ElementAlreadyExistsException
        }
      }

    override def deletePortfolio(userId: Long, portfolioName: String): Future[Unit] =
      dbConnection.run {
        deletePortfolioSQL(userId, portfolioName).map {
          case 0 => throw new NoSuchElementException
          case 1 => ()
          case _ => throw new IllegalStateException()
        }
      }
    override def getUserPortfolios(userId: Long): Future[Seq[Portfolio]] = {
      val request = getUserPortfoliosSQL(userId).flatMap { portfolios =>
        DBIO.sequence(portfolios.map { p =>
          getPortfolioStocksSQL(p.portfolioId).map(stocks =>
            p.copy(stocks = stocks.map(x => (x._2, x._3)).toMap))
        })
      }
      dbConnection.run(request)
    }
    override def getPortfolio(userId: Long, portfolioName: String): Future[Portfolio] = {
      val request = getPortfolioSQL(userId, portfolioName).flatMap { x =>
        DBIO.sequenceOption(x.headOption.map { p =>
          getPortfolioStocksSQL(p.portfolioId).map(stocks =>
            p.copy(stocks = stocks.map(x => (x._2, x._3)).toMap))
        })
      }
      dbConnection.run(request).map(_.get)
    }

    override def addStockToPortfolio(userId: Long, portfolioName: String, stock: String, count: Double): Future[Unit] = {
      val request = getPortfolioSQL(userId, portfolioName).flatMap { x =>
        DBIO.sequenceOption(x.headOption.map(p => addStockToPortfolioSQL(p.portfolioId, stock, count)))
      }
      dbConnection.run {
        request.map {
          case None => throw new NoSuchPortfolioException
          case Some(0) => throw new ElementAlreadyExistsException
          case Some(1) => ()
          case Some(_) => throw new IllegalStateException()
        }
      }
    }

    override def deleteStockFromPortfolio(userId: Long, portfolioName: String, stock: String): Future[Unit] = {
      val request = getPortfolioSQL(userId, portfolioName).flatMap { x =>
        DBIO.sequenceOption(x.headOption.map(p => deleteStockFromPortfolioSQL(p.portfolioId, stock)))
      }
      dbConnection.run {
        request.map {
          case None => throw new NoSuchPortfolioException
          case Some(0) => throw new NoSuchElementException
          case Some(1) => ()
          case Some(_) => throw new IllegalStateException()
        }
      }
    }
    override def getUserNotificationOnAsset(userId: Long, assetType: models.AssetType): Future[Option[DailyNotification]] = {
      val (t, name) = parseAssetType(assetType)
      dbConnection.run(getUserDailyNotificationOnAssetSQL(userId, t, name)).map(_.headOption)
    }
    override def getUserTriggerNotificationOnAsset(userId: Long, assetType: models.AssetType): Future[Seq[TriggerNotification]] = {
      val (t, name) = parseAssetType(assetType)
      dbConnection.run(getUserTriggerNotificationsOnAssetSQL(userId, t, name)).map(seq => seq)
    }

    override def getAllUsers: Future[Seq[User]] =
      dbConnection.run(getUsersSQL)
    override def getUser(userId: Long): Future[Option[User]] =
      dbConnection.run(getUserSQL(userId)).map(_.headOption)
    override def setUser(user: User): Future[Unit] =
      dbConnection.run {
        setUserSQL(user).map {
          case 0 => throw new NoSuchElementException
          case 1 => ()
          case _ => throw new IllegalStateException()
        }
      }

  }

  object PostgresDB {

    private def setId(n: DailyNotification, id: Long): DailyNotification = n match {
      case x: StockDailyNotification => x.copy(id = id)
      case x: ExchangeRateDailyNotification => x.copy(id = id)
      case x: PortfolioDailyNotification => x.copy(id = id)
    }
    private def setId(n: TriggerNotification, id: Long): TriggerNotification = n match {
      case x: StockTriggerNotification => x.copy(id = id)
      case x: ExchangeRateTriggerNotification => x.copy(id = id)
      case x: PortfolioTriggerNotification => x.copy(id = id)
    }

    private def parseAssetType(at: AssetType) = at match {
      case StockAsset(name) => (Stock, name)
      case ExchangeRateAsset(from, to) => (ExchangeRate, s"$from/$to")
      case PortfolioAsset(name) => (Portfolio, name)
    }

  }

}

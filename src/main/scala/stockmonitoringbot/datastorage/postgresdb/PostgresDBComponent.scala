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
      DBConnection.run(getUsersDailyNotificationsSQL(userId))

    override def addDailyNotification(n: DailyNotification): Future[Unit] = {
      DBConnection.run(addDailyNotificationSQL(n)).flatMap {
        case 0 => Future.failed(new ElementAlreadyExistsException)
        case 1 => Future.successful(())
        case _ => Future.failed(new IllegalStateException())
      }
    }
    override def deleteDailyNotification(n: DailyNotification): Future[Unit] = {
      val (name, assetType) = Schema.getNameAndAssetType(n)
      DBConnection.run(deleteDailyNotificationSQL(n.ownerId, assetType, name)).flatMap {
        case 0 => Future.failed(new NoSuchElementException)
        case 1 => Future.successful(())
        case _ => Future.failed(new IllegalStateException())
      }
    }

    override def getUsersTriggerNotifications(userId: Long): Future[Seq[TriggerNotification]] =
      DBConnection.run(getUsersTriggerNotificationsSQL(userId))

    override def addTriggerNotification(n: TriggerNotification): Future[Unit] = {
      DBConnection.run(addTriggerNotificationSQL(n)).flatMap {
        case 0 => Future.failed(new ElementAlreadyExistsException)
        case 1 => Future.successful(())
        case _ => Future.failed(new IllegalStateException())
      }
    }
    override def deleteTriggerNotification(n: TriggerNotification): Future[Unit] = {
      val (name, assetType) = Schema.getNameAndAssetType(n)
      DBConnection.run(deleteTriggerNotificationSQL(n.ownerId, assetType, name, n.boundPrice,
        n.notificationType)).flatMap {
        case 0 => Future.failed(new NoSuchElementException)
        case 1 => Future.successful(())
        case _ => Future.failed(new IllegalStateException())
      }
    }

    override def getAllTriggerNotifications: Future[Iterable[TriggerNotification]] =
      DBConnection.run(getAllTriggerNotificationsSQL)

    override def addPortfolio(p: Portfolio): Future[Unit] =
      DBConnection.run(addPortfolioSQL(p.userId, p.name, p.currency)).flatMap {
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
        DBIO.sequence(portfolios.map { row =>
          getPortfolioStocksSQL(row._1).map(stocks =>
            models.Portfolio(row._2, row._3, row._4, stocks.map(x => (x._2, x._3)).toMap))
        })
      }
      DBConnection.run(request)
    }
    override def getPortfolio(userId: Long, portfolioName: String): Future[Portfolio] = {
      val request = getPortfolioSQL(userId, portfolioName).flatMap { x =>
        DBIO.sequenceOption(x.headOption.map { row =>
          getPortfolioStocksSQL(row._1).map(stocks =>
            models.Portfolio(row._2, row._3, row._4, stocks.map(x => (x._2, x._3)).toMap))
        })
      }
      DBConnection.run(request).map(_.get)
    }

    override def addStockToPortfolio(userId: Long, portfolioName: String, stock: String, count: Double): Future[Unit] = {
      val request = getPortfolioSQL(userId, portfolioName).flatMap { x =>
        DBIO.sequenceOption(x.headOption.map(row => addStockToPortfolioSQL(row._1, stock, count)))
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
        DBIO.sequenceOption(x.headOption.map(row => deleteStockFromPortfolioSQL(row._1, stock)))
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
      DBConnection.run(getUserDailyNotificationOnAssetSQL(userId, t, name)).map(_.headOption)
    }
    override def getUserTriggerNotificationOnAsset(userId: Long, assetType: models.AssetType): Future[Seq[TriggerNotification]] = {
      val (t, name) = parseAssetType(assetType)
      DBConnection.run(getUserTriggerNotificationsOnAssetSQL(userId, t, name)).map(seq => seq)
    }

    override def getUser(userId: Long): Future[Option[User]] =
      DBConnection.run(getUserSQL(userId)).map(_.headOption)
    override def setUser(user: User): Future[Unit] =
      DBConnection.run(setUserSQL(user)).flatMap {
        case 0 => Future.failed(new NoSuchElementException)
        case 1 => Future.successful(())
        case _ => Future.failed(new IllegalStateException())
      }

  }

  object PostgresDB {

    private def parseAssetType(at: AssetType) = at match {
      case StockAsset(name) => (Stock, name)
      case ExchangeRateAsset(from, to) => (ExchangeRate, s"$from/$to")
      case PortfolioAsset(name) => (Portfolio, name)
    }

  }

}

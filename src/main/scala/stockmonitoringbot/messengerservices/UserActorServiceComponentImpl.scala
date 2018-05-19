package stockmonitoringbot.messengerservices

import java.sql.Timestamp

import com.typesafe.scalalogging.Logger
import info.mukel.telegrambot4s.methods.SendMessage
import stockmonitoringbot.{ExecutionContextComponent, notificationhandlers}
import stockmonitoringbot.datastorage.UserDataStorageComponent
import stockmonitoringbot.datastorage.models._
import stockmonitoringbot.notificationhandlers.DailyNotificationHandlerComponent
import stockmonitoringbot.stockpriceservices.models.{CurrencyExchangeRateInfo, StockInfo}
import stockmonitoringbot.stocksandratescache.PriceCacheComponent

import scala.concurrent.Future
import scala.util.Success

/**
  * Created by amir.
  */
trait UserActorServiceComponentImpl extends UserActorServiceComponent {
  this: ExecutionContextComponent
    with UserDataStorageComponent
    with DailyNotificationHandlerComponent
    with PriceCacheComponent
    with MessageSenderComponent =>

  override val userActorService: UserActorService = new UserActorServiceImpl

  class UserActorServiceImpl extends UserActorService {

    private val logger = Logger(getClass)

    override def getUsersDailyNotifications(userId: Long): Future[Seq[DailyNotification]] =
      userDataStorage.getUsersDailyNotifications(userId)
    override def addDailyNotification(notification: DailyNotification): Future[DailyNotification] =
      userDataStorage.addDailyNotification(notification).map { not =>
        dailyNotificationHandler.addDailyNotification(not)
        not
      }
    override def deleteDailyNotification(notificationId: Long): Future[Unit] =
      userDataStorage.deleteDailyNotification(notificationId).map { _ =>
        dailyNotificationHandler.deleteDailyNotification(notificationId)
        ()
      }
    override def getUsersTriggerNotifications(userId: Long): Future[Seq[TriggerNotification]] =
      userDataStorage.getUsersTriggerNotifications(userId)
    override def addTriggerNotification(notification: TriggerNotification): Future[TriggerNotification] =
      userDataStorage.addTriggerNotification(notification)
    override def deleteTriggerNotification(notificationId: Long): Future[Unit] =
      userDataStorage.deleteTriggerNotification(notificationId)
    override def addPortfolio(portfolio: Portfolio): Future[Portfolio] =
      userDataStorage.addPortfolio(portfolio)
    override def deletePortfolio(userId: Long, portfolioName: String): Future[Unit] = {
      val asset = PortfolioAsset(portfolioName)
      val clearDailyNotification = userDataStorage.getUserNotificationOnAsset(userId, asset).map(_.fold(()) { not =>
        deleteDailyNotification(not.id)
        ()
      })
      val clearTriggerNotifications = for {
        triggerNots <- userDataStorage.getUserTriggerNotificationOnAsset(userId, asset)
        _ <- Future.traverse(triggerNots)(not => deleteTriggerNotification(not.id))
      } yield ()
      for {_ <- clearDailyNotification
           _ <- clearTriggerNotifications
           _ <- userDataStorage.deletePortfolio(userId, portfolioName)
      } yield ()
    }
    override def getUserPortfolios(userId: Long): Future[Seq[Portfolio]] =
      userDataStorage.getUserPortfolios(userId)
    override def getPortfolio(userId: Long, portfolioName: String): Future[Portfolio] =
      userDataStorage.getPortfolio(userId, portfolioName)
    override def addStockToPortfolio(userId: Long, portfolioName: String, stock: String, count: Double): Future[Unit] =
      userDataStorage.addStockToPortfolio(userId, portfolioName, stock, count)
    override def deleteStockFromPortfolio(userId: Long, portfolioName: String, stock: String): Future[Unit] =
      userDataStorage.deleteStockFromPortfolio(userId, portfolioName, stock)
    override def getUserNotificationOnAsset(userId: Long, assetType: AssetType): Future[Option[DailyNotification]] =
      userDataStorage.getUserNotificationOnAsset(userId, assetType)
    override def getUserTriggerNotificationOnAsset(userId: Long, assetType: AssetType): Future[Seq[TriggerNotification]] =
      userDataStorage.getUserTriggerNotificationOnAsset(userId, assetType)

    override def getUser(userId: Long): Future[Option[User]] = userDataStorage.getUser(userId)
    override def setUser(user: User): Future[Unit] = userDataStorage.setUser(user)

    override def getStockInfo(stock: String, userId: Long): Future[StockInfo] = {
      val stockFut = priceCache.getStockInfo(stock)
      stockFut.onComplete {
        case Success(_) =>
          val query = UserQuery(userId, StockAsset(stock), new Timestamp(System.currentTimeMillis()))
          userDataStorage.addQueryToHistory(query).failed.foreach { e =>
            logger.error(s"Can't save query to history $query", e)
          }
        case _ =>
      }
      stockFut
    }
    override def cacheContains(stock: String): Boolean = priceCache.contains(stock)
    override def getExchangeRate(from: String, to: String, userId: Long): Future[CurrencyExchangeRateInfo] = {
      val rateFut = priceCache.getExchangeRate(from, to)
      rateFut.onComplete {
        case Success(_) =>
          val query = UserQuery(userId, ExchangeRateAsset(from, to), new Timestamp(System.currentTimeMillis()))
          userDataStorage.addQueryToHistory(query).failed.foreach { e =>
            logger.error(s"Can't save query to history $query", e)
          }
        case _ =>
      }
      rateFut
    }

    override def sendMessage(sendMessage: SendMessage): Unit = messageSender(sendMessage)

    override def getPortfolioCurrentPrice(p: Portfolio): Future[BigDecimal] =
      notificationhandlers.getPortfolioCurrentPrice(p, priceCache)
    override def getPortfolioStocksPrice(portfolio: Portfolio): Future[Map[String, BigDecimal]] = {
      Future.traverse(portfolio.stocks.keys)(priceCache.getStockInfo).map {
        _.map(stock => (stock.name, stock.price * portfolio.stocks(stock.name))).toMap
      }
    }
    override def getStockQueryHistory(userId: Long, num: Int): Future[Seq[UserQuery]] =
      userDataStorage.getHistory(userId, StockAsset(""), num)
    override def getExchangeRateQueryHistory(userId: Long, num: Int): Future[Seq[UserQuery]] =
      userDataStorage.getHistory(userId, ExchangeRateAsset("", ""), num)
  }

}

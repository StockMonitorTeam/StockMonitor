package stockmonitoringbot.notificationhandlers

import java.time.{LocalTime, ZoneId}
import java.util.concurrent.ConcurrentHashMap

import akka.actor.Cancellable
import com.typesafe.scalalogging.Logger
import info.mukel.telegrambot4s.methods.SendMessage
import stockmonitoringbot.datastorage.UserDataStorageComponent
import stockmonitoringbot.datastorage.models._
import stockmonitoringbot.messengerservices.MessageSenderComponent
import stockmonitoringbot.messengerservices.markups.GeneralTexts
import stockmonitoringbot.stocksandratescache.PriceCacheComponent
import stockmonitoringbot.{ActorSystemComponent, ExecutionContextComponent}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

/**
  * Created by amir.
  */
trait DailyNotificationHandlerComponentImpl extends DailyNotificationHandlerComponent {
  this: UserDataStorageComponent
    with PriceCacheComponent
    with MessageSenderComponent
    with ExecutionContextComponent
    with ActorSystemComponent =>

  val dailyNotificationHandler = new DailyNotificationHandlerImpl

  class DailyNotificationHandlerImpl extends DailyNotificationHandler {

    private val logger = Logger(getClass)

    private val notifications = new ConcurrentHashMap[Long, Cancellable]()

    private def makeNotificationMessage(notification: DailyNotification): Future[String] = notification match {
      case StockDailyNotification(_, _, stock, _) =>
        priceCache.getStockInfo(stock).map { stockInfo =>
          GeneralTexts.DAILY_NOTIFICATION_STOCK_INFO(stockInfo)
        }
      case ExchangeRateDailyNotification(_, _, (from, to), _) =>
        priceCache.getExchangeRate(from, to).map { exchangeRateInfo =>
          GeneralTexts.DAILY_NOTIFICATION_EXCHANGE_RATE_INFO(exchangeRateInfo)
        }
      case PortfolioDailyNotification(_, userId, portfolioName, _) =>
        for {portfolio <- userDataStorage.getPortfolio(userId, portfolioName)
             portfolioPrice <- getPortfolioCurrentPrice(portfolio, priceCache)
        } yield GeneralTexts.DAILY_NOTIFICATION_PORTFOLIO_INFO(portfolio.name, portfolioPrice)
    }

    private val secondsInDay: Int = 24 * 60 * 60
    private def untilTime(time: LocalTime): FiniteDuration = {
      val nowSec = LocalTime.now(ZoneId.of("UTC")).toSecondOfDay
      val timeSec = time.toSecondOfDay
      val dif = (timeSec + secondsInDay - nowSec) % secondsInDay
      dif seconds
    }

    def notifyUser(notification: DailyNotification): Future[Unit] = {
      val notificationSend = makeNotificationMessage(notification).map { message =>
        messageSender(SendMessage(notification.ownerId, message))
      }
      notificationSend.failed.map {
        exception => logger.error(s"can't make notification message on notification $notification", exception)
      }
      notificationSend
    }

    def addDailyNotification(notification: DailyNotification): Unit = {
      val prev = notifications.put(notification.id,
        system.scheduler.schedule(untilTime(notification.time), 24 hours)(notifyUser(notification)))
      Option(prev).foreach(_.cancel())
    }

    def deleteDailyNotification(id: Long): Unit = {
      Option(notifications.remove(id)).foreach(_.cancel())
    }

    def init(): Future[Unit] = {
      val initFuture = userDataStorage.getAllDailyNotifications.map { notifications =>
        notifications.foreach(notification => addDailyNotification(notification))
        notifications.size
      }
      initFuture.onComplete {
        case Success(n) =>
          logger.info(s"loaded $n daily notifications into scheduler")
        case Failure(e) =>
          logger.error("Can't init DailyNotificationHandler", e)
      }
      initFuture.map(_ => ())
    }

  }
}
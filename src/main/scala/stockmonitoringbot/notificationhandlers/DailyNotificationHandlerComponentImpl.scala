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

    private val notifications = new ConcurrentHashMap[DailyNotification, Cancellable]()

    private def makeNotificationMessage(notification: DailyNotification): Future[String] = notification match {
      case StockDailyNotification(_, stock, _) =>
        priceCache.getStockInfo(stock).map { stockInfo =>
          GeneralTexts.DAILY_NOTIFICATION_STOCK_INFO(stockInfo)
        }
      case ExchangeRateDailyNotification(_, (from, to), _) =>
        priceCache.getExchangeRate(from, to).map { exchangeRateInfo =>
          GeneralTexts.DAILY_NOTIFICATION_EXCHANGE_RATE_INFO(exchangeRateInfo)
        }
      case PortfolioDailyNotification(userId, portfolioName, _) =>
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
      val prev = notifications.put(notification,
        system.scheduler.schedule(untilTime(notification.time), 24 hours)(notifyUser(notification)))
      Option(prev).foreach(_.cancel())
    }

    def deleteDailyNotification(notification: DailyNotification): Unit = {
      Option(notifications.remove(notification)).foreach(_.cancel())
    }

  }
}
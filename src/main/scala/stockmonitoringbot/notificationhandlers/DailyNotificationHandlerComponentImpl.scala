package stockmonitoringbot.notificationhandlers

import java.time.{LocalTime, ZoneId}
import java.util.concurrent.ConcurrentHashMap

import akka.actor.Cancellable
import com.typesafe.scalalogging.Logger
import info.mukel.telegrambot4s.methods.SendMessage
import stockmonitoringbot.datastorage.UserDataStorageComponent
import stockmonitoringbot.datastorage.models._
import stockmonitoringbot.messengerservices.MessageSenderComponent
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
          s"${stockInfo.name} price is ${stockInfo.price}(last refresh: ${stockInfo.lastRefreshed})"
        }
      case ExchangeRateDailyNotification(_, (from, to), _) =>
        priceCache.getExchangeRate(from, to).map { exchangeRateInfo =>
          s"${exchangeRateInfo.from}/${exchangeRateInfo.to} exchange rate is ${exchangeRateInfo.rate}" +
            s"(last refresh: ${exchangeRateInfo.lastRefreshed})" +
            s"\n ${exchangeRateInfo.from} - ${exchangeRateInfo.descriptionFrom}" +
            s"\n ${exchangeRateInfo.to} - ${exchangeRateInfo.descriptionTo}"
        }
      case PortfolioDailyNotification(userId, portfolioName, _) =>
        for {portfolio <- userDataStorage.getPortfolio(userId, portfolioName)
             portfolioPrice <- getPortfolioCurrentPrice(portfolio, priceCache)
        } yield s"your portfolio $q${portfolio.name}$q current price is $portfolioPrice"
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
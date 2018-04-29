package stockmonitoringbot.notificationhandlers

import java.time.{LocalTime, ZoneId}

import akka.actor.Cancellable
import com.typesafe.scalalogging.Logger
import info.mukel.telegrambot4s.methods.SendMessage
import stockmonitoringbot.datastorage.UserDataStorageComponent
import stockmonitoringbot.datastorage.models._
import stockmonitoringbot.messengerservices.MessageSenderComponent
import stockmonitoringbot.stocksandratescache.PriceCacheComponent
import stockmonitoringbot.{ActorSystemComponent, ExecutionContextComponent}

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

/**
  * Created by amir.
  */
trait DailyNotificationHandlerComponentImpl extends DailyNotificationHandlerComponent {
  self: UserDataStorageComponent
    with PriceCacheComponent
    with MessageSenderComponent
    with ExecutionContextComponent
    with ActorSystemComponent =>

  val dailyNotificationHandler = new DailyNotificationHandlerImpl

  class DailyNotificationHandlerImpl extends DailyNotificationHandler {

    private val logger = Logger(getClass)

    private val notifications = mutable.Map.empty[DailyNotification, Cancellable]

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

    val secondsInDay: Int = 24 * 60 * 60
    def untilTime(time: LocalTime): FiniteDuration = {
      val nowSec = LocalTime.now(ZoneId.of("UTC")).toSecondOfDay
      val timeSec = time.toSecondOfDay
      val dif = (timeSec + secondsInDay - nowSec) % secondsInDay
      dif seconds
    }

    def addDailyNotification(notification: DailyNotification): Unit = {
      notifications += notification -> system.scheduler.schedule(untilTime(notification.time), 24 hours) {
        makeNotificationMessage(notification).onComplete {
          case Success(message) =>
            messageSender(SendMessage(notification.ownerId, message))
          case Failure(exception) => logger.error(s"can't make notification message on notification $notification: $exception")
        }
      }
    }

    def deleteDailyNotification(notification: DailyNotification): Unit = {
      notifications.get(notification).foreach(_.cancel())
      notifications -= notification
    }

  }
}
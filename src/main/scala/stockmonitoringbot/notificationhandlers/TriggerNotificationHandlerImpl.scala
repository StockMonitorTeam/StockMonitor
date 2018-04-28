package stockmonitoringbot.notificationhandlers

import com.typesafe.scalalogging.Logger
import info.mukel.telegrambot4s.methods.SendMessage
import stockmonitoringbot.datastorage.UserDataStorage
import stockmonitoringbot.datastorage.models._
import stockmonitoringbot.messengerservices.MessageSender
import stockmonitoringbot.stockpriceservices.StockPriceService
import stockmonitoringbot.stocksandratescache.StocksAndExchangeRatesCache
import stockmonitoringbot.{ActorSystemComponent, ExecutionContextComponent}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

/**
  * Created by amir.
  */
trait TriggerNotificationHandlerImpl extends TriggerNotificationHandler {
  self: StockPriceService
    with UserDataStorage
    with StocksAndExchangeRatesCache
    with MessageSender
    with ExecutionContextComponent
    with ActorSystemComponent =>

  private val logger = Logger(getClass)

  /**
    *
    * @return number of updated stocks & exchange rates
    */
  def updateCache(): Future[(Int, Int)] = {
    val stockUpdate: Future[Int] = getBatchPrices(getStocks.toSeq).map { updatedStocks =>
      updatedStocks.foreach(setStockInfo)
      updatedStocks.size
    }
    val ratesUpdate: Future[Int] = Future.traverse(getExchangePairs) { pair =>
      getCurrencyExchangeRate(pair._1, pair._2).map(setExchangeRate)
    }.map(_.size)
    for {numOfUpdatedStocks <- stockUpdate
         numOfUpdatedRates <- ratesUpdate
    } yield (numOfUpdatedStocks, numOfUpdatedRates)
  }

  private def isTriggeredWithPrice(notification: TriggerNotification, price: BigDecimal): Option[(TriggerNotification, BigDecimal)] = {
    notification.notificationType match {
      case RaiseNotification if price >= notification.boundPrice =>
        Some((notification, price))
      case FallNotification if price <= notification.boundPrice =>
        Some((notification, price))
      case _ => None
    }
  }

  /**
    *
    * @param notification
    * @return None, if notification is not triggered, Some((notification, price)), if notification is triggered, price - current price
    */
  private def isTriggered(notification: TriggerNotification): Future[Option[(TriggerNotification, BigDecimal)]] = notification match {
    case StockTriggerNotification(_, stock, _, _) =>
      getStockInfo(stock).map { stockInfo =>
        isTriggeredWithPrice(notification, stockInfo.price)
      }
    case ExchangeRateTriggerNotification(_, (from, to), _, _) =>
      getExchangeRate(from, to).map { exchangeRateInfo =>
        isTriggeredWithPrice(notification, exchangeRateInfo.rate)
      }
    case PortfolioTriggerNotification(userId, portfolioName, _, _) =>
      for {portfolio <- getPortfolio(userId, portfolioName)
           portfolioPrice <- getPortfolioCurrentPrice(portfolio, this)
      } yield
        isTriggeredWithPrice(notification, portfolioPrice)
  }

  private def makeTriggerMessage(notification: TriggerNotification, price: BigDecimal): String = {
    case StockTriggerNotification(_, stock, bound, notificationType) =>
      val x = notificationType match {
        case RaiseNotification =>
          "higher"
        case FallNotification =>
          "lower"
      }
      s"Your notification is triggered! $stock is $x than $bound. It's current price $price"
    case ExchangeRateTriggerNotification(_, (from, to), bound, notificationType) =>
      val x = notificationType match {
        case RaiseNotification =>
          "higher"
        case FallNotification =>
          "lower"
      }
      s"Your notification is triggered! $from/$to exchange rate is $x than $bound. It's $price"
    case PortfolioTriggerNotification(_, portfolioName, bound, notificationType) =>
      val x = notificationType match {
        case RaiseNotification =>
          "higher"
        case FallNotification =>
          "lower"
      }
      s"Your notification is triggered! portfolio \"$portfolioName\" is $x than $bound. It's current price $price"
  }

  def start() {
    // update every minute StocksAndExchangeRatesCache
    // and check all trigger notifications
    system.scheduler.schedule(1 second, 1 minute) {
      logger.info("Starting cache update...")
      updateCache().onComplete {
        case Success((numOfStocks, numOfRates)) =>
          logger.info(s"Cache successfully updated. Updated $numOfStocks stocks & $numOfRates exchange rates.")
          for {notifications <- getAllTriggerNotifications
               triggeredNotifications <- Future.traverse(notifications)(isTriggered)
          } {
            triggeredNotifications.flatten.foreach { notification =>
              send(SendMessage(notification._1.ownerId, makeTriggerMessage(notification._1, notification._2)))
            }
          }
        case Failure(exception) => logger.error(s"Can't update cache: $exception")
      }
    }
  }
}
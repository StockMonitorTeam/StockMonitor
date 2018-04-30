package stockmonitoringbot.notificationhandlers

import akka.actor.Cancellable
import com.typesafe.scalalogging.Logger
import info.mukel.telegrambot4s.methods.SendMessage
import stockmonitoringbot.datastorage.UserDataStorageComponent
import stockmonitoringbot.datastorage.models._
import stockmonitoringbot.messengerservices.MessageSenderComponent
import stockmonitoringbot.stockpriceservices.StockPriceServiceComponent
import stockmonitoringbot.stocksandratescache.PriceCacheComponent
import stockmonitoringbot.{ActorSystemComponent, ExecutionContextComponent}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

/**
  * Created by amir.
  */
trait TriggerNotificationHandlerComponentImpl extends TriggerNotificationHandlerComponent {
  self: StockPriceServiceComponent
    with UserDataStorageComponent
    with PriceCacheComponent
    with MessageSenderComponent
    with ExecutionContextComponent
    with ActorSystemComponent =>

  val triggerNotificationHandler = new TriggerNotificationHandlerImpl

  class TriggerNotificationHandlerImpl extends TriggerNotificationHandler {

    private val logger = Logger(getClass)
    private var mainTask: Cancellable = _

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
      * @return None, if notification is not triggered, Some((notification, price)), if notification is triggered, price - current price
      */
    private def isTriggered(notification: TriggerNotification): Future[Option[(TriggerNotification, BigDecimal)]] = notification match {
      case StockTriggerNotification(_, stock, _, _) =>
        priceCache.getStockInfo(stock).map { stockInfo =>
          isTriggeredWithPrice(notification, stockInfo.price)
        }
      case ExchangeRateTriggerNotification(_, (from, to), _, _) =>
        priceCache.getExchangeRate(from, to).map { exchangeRateInfo =>
          isTriggeredWithPrice(notification, exchangeRateInfo.rate)
        }
      case PortfolioTriggerNotification(userId, portfolioName, _, _) =>
        for {portfolio <- userDataStorage.getPortfolio(userId, portfolioName)
             portfolioPrice <- getPortfolioCurrentPrice(portfolio, priceCache)
        } yield
          isTriggeredWithPrice(notification, portfolioPrice)
    }

    private def makeTriggerMessage(notification: TriggerNotification, price: BigDecimal): String = notification match {
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
        s"Your notification is triggered! portfolio $q$portfolioName$q is $x than $bound. It's current price $price"
    }

    /**
      *
      * @return number of updated stocks & exchange rates
      */
    def updateCache(): Future[(Int, Int)] = {
      val stockUpdate: Future[Int] = stockPriceService.getBatchPrices(priceCache.getStocks.toSeq).map { updatedStocks =>
        updatedStocks.foreach(priceCache.setStockInfo)
        updatedStocks.size
      }
      val ratesUpdate: Future[Int] = Future.traverse(priceCache.getExchangePairs) { pair =>
        stockPriceService.getCurrencyExchangeRate(pair._1, pair._2).map(priceCache.setExchangeRate)
      }.map(_.size)
      for {numOfUpdatedStocks <- stockUpdate
           numOfUpdatedRates <- ratesUpdate
      } yield (numOfUpdatedStocks, numOfUpdatedRates)
    }

    def checkTriggers(): Future[Unit] = {
      for {notifications <- userDataStorage.getAllTriggerNotifications
           triggeredNotifications <- Future.traverse(notifications)(isTriggered)
      } yield {
        triggeredNotifications.flatten.foreach { notification =>
          messageSender(SendMessage(notification._1.ownerId, makeTriggerMessage(notification._1, notification._2)))
        }
        ()
      }
    }

    private def updateCacheAndCheckTriggers(): Unit = {
      logger.info("Starting cache update...")
      updateCache().onComplete {
        case Success((numOfStocks, numOfRates)) =>
          logger.info(s"Cache successfully updated. Updated $numOfStocks stocks & $numOfRates exchange rates.")
          checkTriggers()
        case Failure(exception) => logger.error(s"Can't update cache: $exception")
      }
    }

    override def start(): Unit = {
      // update every minute StocksAndExchangeRatesCache
      // and check all trigger notifications
      //todo load "updateFrequency" from config
      mainTask = system.scheduler.schedule(1 second, 1 minute)(updateCacheAndCheckTriggers())
    }

    override def stop(): Boolean = mainTask.cancel()
  }

}
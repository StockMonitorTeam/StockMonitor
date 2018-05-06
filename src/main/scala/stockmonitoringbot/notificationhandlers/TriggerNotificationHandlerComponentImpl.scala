package stockmonitoringbot.notificationhandlers

import akka.actor.Cancellable
import com.typesafe.scalalogging.Logger
import info.mukel.telegrambot4s.methods.SendMessage
import stockmonitoringbot.datastorage.UserDataStorageComponent
import stockmonitoringbot.datastorage.models._
import stockmonitoringbot.messengerservices.MessageSenderComponent
import stockmonitoringbot.stockpriceservices.StockPriceServiceComponent
import stockmonitoringbot.stocksandratescache.{PriceCache, PriceCacheComponent}
import stockmonitoringbot.{ActorSystemComponent, ExecutionContextComponent}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

/**
  * Created by amir.
  */
trait TriggerNotificationHandlerComponentImpl extends TriggerNotificationHandlerComponent {
  this: StockPriceServiceComponent
    with UserDataStorageComponent
    with PriceCacheComponent
    with MessageSenderComponent
    with ExecutionContextComponent
    with ActorSystemComponent =>

  val triggerNotificationHandler = new TriggerNotificationHandlerImpl

  class TriggerNotificationHandlerImpl extends TriggerNotificationHandler {

    private val logger = Logger(getClass)
    private var mainTask: Cancellable = _

    private def isTriggeredWithPrice(notification: TriggerNotification,
                                     lastPrice: BigDecimal,
                                     currentPrice: BigDecimal): Option[(TriggerNotification, BigDecimal)] = {
      notification.notificationType match {
        case RaiseNotification if currentPrice >= notification.boundPrice =>
          Some((notification, currentPrice))
        case FallNotification if currentPrice <= notification.boundPrice =>
          Some((notification, currentPrice))
        case BothNotification if currentPrice <= notification.boundPrice ^ lastPrice <= notification.boundPrice =>
          Some((notification, currentPrice))
        case _ => None
      }
    }

    /**
      *
      * @return None, if notification is not triggered, Some((notification, price)), if notification is triggered, price - current price
      */
    private def isTriggered(oldCache: PriceCache, newCache: PriceCache)(notification: TriggerNotification): Future[Option[(TriggerNotification, BigDecimal)]] = notification match {
      case StockTriggerNotification(_, stock, _, _) =>
        val oldPriceFuture = oldCache.getStockInfo(stock)
        val newPriceFuture = newCache.getStockInfo(stock)
        for {oldPrice <- oldPriceFuture
             newPrice <- newPriceFuture}
          yield isTriggeredWithPrice(notification, oldPrice.price, newPrice.price)
      case ExchangeRateTriggerNotification(_, (from, to), _, _) =>
        val oldRateFuture = oldCache.getExchangeRate(from, to)
        val newRateFuture = newCache.getExchangeRate(from, to)
        for {oldRate <- oldRateFuture
             newRate <- newRateFuture}
          yield isTriggeredWithPrice(notification, oldRate.rate, newRate.rate)
      case PortfolioTriggerNotification(userId, portfolioName, _, _) =>
        val portfolioFuture = userDataStorage.getPortfolio(userId, portfolioName)
        val oldPriceFuture = for {portfolio <- portfolioFuture
                                  portfolioPrice <- getPortfolioCurrentPrice(portfolio, oldCache)}
          yield portfolioPrice
        val newPriceFuture = for {portfolio <- portfolioFuture
                                  portfolioPrice <- getPortfolioCurrentPrice(portfolio, newCache)}
          yield portfolioPrice
        (for {oldPrice <- oldPriceFuture
              newPrice <- newPriceFuture
        } yield isTriggeredWithPrice(notification, oldPrice, newPrice)).recover {
          case _: NoSuchElementException =>
            logger.warn(s"notification $notification exists, but portfolio doesn't")
            None
        }
    }

    private def notificationTypeMessage(x: TriggerNotificationType, bound: BigDecimal): String = x match {
      case RaiseNotification =>
        s"поднялась выше $bound"
      case FallNotification =>
        s"опустилась ниже $bound"
      case BothNotification =>
        s"достигла порога: $bound"
    }

    private def makeTriggerMessage(notification: TriggerNotification, price: BigDecimal): String = notification match {
      case StockTriggerNotification(_, stock, bound, notificationType) =>
        val msg = notificationTypeMessage(notificationType, bound)
        s"Сработало триггер оповещение! Стоимость $stock $msg. Текущая цена $price"
      case ExchangeRateTriggerNotification(_, (from, to), bound, notificationType) =>
        val msg = notificationTypeMessage(notificationType, bound)
        s"Сработало триггер оповещение! Цена валютной пары $from/$to $msg. Текущая цена: $price"
      case PortfolioTriggerNotification(_, portfolioName, bound, notificationType) =>
        val msg = notificationTypeMessage(notificationType, bound)
        s"Сработало триггер оповещение! Стоимость портеля «$portfolioName» $msg. Текущая цена: $price"
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

    def checkTriggers(newCache: PriceCache, oldCache: PriceCache): Future[Unit] = {
      for {notifications <- userDataStorage.getAllTriggerNotifications
           triggeredNotifications <- Future.traverse(notifications)(isTriggered(oldCache, newCache))
      } yield {
        triggeredNotifications.flatten.foreach { notification =>
          messageSender(SendMessage(notification._1.ownerId, makeTriggerMessage(notification._1, notification._2)))
          userDataStorage.deleteTriggerNotification(notification._1)
        }
        ()
      }
    }

    private def updateCacheAndCheckTriggers(): Unit = {
      val oldCache = priceCache.copy()
      logger.info("Starting cache update...")
      updateCache().onComplete {
        case Success((numOfStocks, numOfRates)) =>
          logger.info(s"Cache successfully updated. Updated $numOfStocks stocks & $numOfRates exchange rates.")
          checkTriggers(priceCache, oldCache)
        case Failure(exception) => logger.error(s"Can't update cache", exception)
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
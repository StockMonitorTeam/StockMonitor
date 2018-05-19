package stockmonitoringbot.notificationhandlers

import java.util.concurrent.ConcurrentHashMap

import akka.actor.Cancellable
import com.typesafe.scalalogging.Logger
import info.mukel.telegrambot4s.methods.SendMessage
import stockmonitoringbot.datastorage.UserDataStorageComponent
import stockmonitoringbot.datastorage.models._
import stockmonitoringbot.messengerservices.MessageSenderComponent
import stockmonitoringbot.messengerservices.markups.GeneralTexts
import stockmonitoringbot.stockpriceservices.StockPriceServiceComponent
import stockmonitoringbot.stocksandratescache.PriceCacheComponent
import stockmonitoringbot.{ActorSystemComponent, AppConfig, ExecutionContextComponent}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

/**
  * Created by amir.
  */
trait TriggerNotificationHandlerComponentImpl extends TriggerNotificationHandlerComponent {
  this: StockPriceServiceComponent
    with UserDataStorageComponent
    with PriceCacheComponent
    with MessageSenderComponent
    with ExecutionContextComponent
    with ActorSystemComponent
    with AppConfig =>

  val triggerNotificationHandler = new TriggerNotificationHandlerImpl
  val oldPrices = new ConcurrentHashMap[Long, BigDecimal]()

  class TriggerNotificationHandlerImpl extends TriggerNotificationHandler {

    private val logger = Logger(getClass)
    private var mainTask: Cancellable = _
    private val updateFrequency = getKey("StockMonitor.cacheUpdateFrequency")

    private def isTriggeredWithPrice(notification: TriggerNotification,
                                     lastPrice: BigDecimal,
                                     currentPrice: BigDecimal): Option[(TriggerNotification, BigDecimal)] = {
      notification.notificationType match {
        case Raise if currentPrice >= notification.boundPrice =>
          Some((notification, currentPrice))
        case Fall if currentPrice <= notification.boundPrice =>
          Some((notification, currentPrice))
        case Both if currentPrice <= notification.boundPrice ^ lastPrice <= notification.boundPrice =>
          Some((notification, currentPrice))
        case _ => None
      }
    }

    /**
      *
      * @return None, if notification is not triggered, Some((notification, price)), if notification is triggered, price - current price
      */
    private def isTriggered(notification: TriggerNotification): Future[Option[(TriggerNotification, BigDecimal)]] = notification match {
      case StockTriggerNotification(_, _, stock, _, _) =>
        priceCache.getStockInfo(stock).map { info =>
          val oldPrice = Option(oldPrices.put(notification.id, info.price))
          isTriggeredWithPrice(notification, oldPrice.getOrElse(info.price), info.price)
        }
      case ExchangeRateTriggerNotification(_, _, (from, to), _, _) =>
        priceCache.getExchangeRate(from, to).map { info =>
          val oldPrice = Option(oldPrices.put(notification.id, info.rate))
          isTriggeredWithPrice(notification, oldPrice.getOrElse(info.rate), info.rate)
        }
      case PortfolioTriggerNotification(_, userId, portfolioName, _, _) =>
        (for {portfolio <- userDataStorage.getPortfolio(userId, portfolioName)
              portfolioPrice <- getPortfolioCurrentPrice(portfolio, priceCache)} yield {
          val oldPrice = Option(oldPrices.put(notification.id, portfolioPrice))
          isTriggeredWithPrice(notification, oldPrice.getOrElse(portfolioPrice), portfolioPrice)
        }).recover {
          case _: NoSuchElementException =>
            logger.warn(s"notification $notification exists, but portfolio doesn't")
            None
        }
    }

    def checkTriggers(): Future[Unit] = {
      for {notifications <- userDataStorage.getAllTriggerNotifications
           triggeredNotifications <- Future.traverse(notifications)(isTriggered)
      } yield {
        triggeredNotifications.flatten.foreach { notification =>
          messageSender(SendMessage(notification._1.ownerId, GeneralTexts.TRIGGER_MESSAGE(notification._1, notification._2)))
          userDataStorage.deleteTriggerNotification(notification._1.id)
        }
        ()
      }
    }

    private def updateCacheAndCheckTriggers(): Unit = {
      logger.info("Starting cache update...")
      priceCache.updateCache().onComplete {
        case Success((numOfStocks, numOfRates)) =>
          logger.info(s"Cache successfully updated. Updated $numOfStocks stocks & $numOfRates exchange rates.")
          checkTriggers()
        case Failure(exception) => logger.error(s"Can't update cache", exception)
      }
    }

    override def start(): Unit = {
      val freq = Try(Duration(updateFrequency).asInstanceOf[FiniteDuration]).fold({
        _ =>
          logger.warn("Can't get update frequency from configuration, starting cache update with frequency 1 minute")
          1 minute
      }, {
        f =>
          logger.info(s"starting cache update with frequency $f")
          f
      })
      // update every minute StocksAndExchangeRatesCache
      // and check all trigger notifications
      mainTask = system.scheduler.schedule(freq, 1 minute)(updateCacheAndCheckTriggers())
    }

    override def stop(): Boolean = mainTask.cancel()
  }

}
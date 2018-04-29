package stockmonitoringbot

import com.typesafe.scalalogging.Logger
import stockmonitoringbot.datastorage.UserDataStorageComponent
import stockmonitoringbot.messengerservices.MessageSenderComponent
import stockmonitoringbot.stockpriceservices.StockPriceServiceComponent
import stockmonitoringbot.stocksandratescache.PriceCacheComponent

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

/**
  * Created by amir.
  */
trait StockMonitoringBot {
  self: StockPriceServiceComponent
    with UserDataStorageComponent
    with PriceCacheComponent
    with MessageSenderComponent
    with ExecutionContextComponent
    with ActorSystemComponent =>

  private val logger = Logger(getClass)

  def updateStockPrices(): Unit = {
    stockPriceService.getBatchPrices(priceCache.getStocks.toSeq).map(_.foreach(priceCache.setStockInfo))
    //todo check triggered notifications and send notifications to users
  }

  def updateExchangeRates(): Unit = {
    priceCache.getExchangePairs.foreach { pair =>
      stockPriceService.getCurrencyExchangeRate(pair._1, pair._2).onComplete {
        case Success(exchangeRate) => priceCache.setExchangeRate(exchangeRate)
        case Failure(exception) => logger.error(s"Can't update $pair: $exception")
      }
    }
    //todo check triggered notifications and send notifications to users
  }

  //todo schedule daily notifications

  //update every minute StocksAndExchangeRatesCache
  system.scheduler.schedule(1 second, 1 minute)(updateStockPrices())
  system.scheduler.schedule(1 second, 1 minute)(updateExchangeRates())

}
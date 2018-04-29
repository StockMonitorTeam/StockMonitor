package stockmonitoringbot

import com.typesafe.scalalogging.Logger
import stockmonitoringbot.datastorage.UserDataStorageComponent
import stockmonitoringbot.messengerservices.MessageSender
import stockmonitoringbot.stockpriceservices.StockPriceService
import stockmonitoringbot.stocksandratescache.StocksAndExchangeRatesCache

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

/**
  * Created by amir.
  */
trait StockMonitoringBot {
  self: StockPriceService
    with UserDataStorageComponent
    with StocksAndExchangeRatesCache
    with MessageSender
    with ExecutionContextComponent
    with ActorSystemComponent =>

  private val logger = Logger(getClass)

  def updateStockPrices(): Unit = {
    getBatchPrices(getStocks.toSeq).map(_.foreach(setStockInfo))
    //todo check triggered notifications and send notifications to users
  }

  def updateExchangeRates(): Unit = {
    getExchangePairs.foreach { pair =>
      getCurrencyExchangeRate(pair._1, pair._2).onComplete {
        case Success(exchangeRate) => setExchangeRate(exchangeRate)
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
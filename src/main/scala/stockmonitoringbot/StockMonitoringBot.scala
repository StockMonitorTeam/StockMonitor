package stockmonitoringbot

import info.mukel.telegrambot4s.methods.SendMessage
import stockmonitoringbot.datastorage.DataStorage
import stockmonitoringbot.messengerservices.TelegramService
import stockmonitoringbot.stockpriceservices.StockPriceService

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by amir.
  */
trait StockMonitoringBot {
  self: StockPriceService
    with DataStorage
    with TelegramService
    with ExecutionContextComponent
    with ActorSystemComponent =>

  def updatePrices(): Unit =
    for {stocks <- getStocks
         stockName <- stocks
         stockInfo <- getStockPriceInfo(stockName)
         _ = logger.info(s"Updating $stockName, new price is ${stockInfo.price}")
         triggeredNotifications <- updateStockPrice(stockInfo.name, stockInfo.price)
         triggeredNotification <- triggeredNotifications
    } {
      send(SendMessage(triggeredNotification.userId, s"${stockInfo.name} price is ${stockInfo.price} now"))
    }

  system.scheduler.schedule(1 second, 1 minute)(updatePrices())

}
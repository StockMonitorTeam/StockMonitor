package stockmonitoringbot

import stockmonitoringbot.datastorage.DataStorageWithStockInfoLoad
import stockmonitoringbot.messengerservices.TelegramService
import stockmonitoringbot.stockpriceservices.AlphavantageStockPriceService

/**
  * Created by amir.
  */
object Main extends App {

  val bot = new {}
    with StockMonitoringBot
    with DataStorageWithStockInfoLoad
    with AlphavantageStockPriceService
    with TelegramService
    with ExecutionContextImpl
    with ActorSystemComponentImpl

  bot.run()

}

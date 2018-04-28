package stockmonitoringbot

import stockmonitoringbot.datastorage.DataStorageWithStockInfoLoad
import stockmonitoringbot.messengerservices.TelegramService
import stockmonitoringbot.stockpriceservices.{AlphavantageHttpRequestExecutor, AlphavantageStockPriceService}

/**
  * Created by amir.
  */
object Main extends App {

  val bot = new {}
    with StockMonitoringBot
    with DataStorageWithStockInfoLoad
    with AlphavantageStockPriceService
    with AlphavantageHttpRequestExecutor
    with TelegramService
    with ExecutionContextImpl
    with ActorSystemComponentImpl
    with ApiKeysImpl
  bot.run()

}

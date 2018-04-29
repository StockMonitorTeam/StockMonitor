package stockmonitoringbot

import stockmonitoringbot.datastorage.InMemoryUserDataStorageComponentImpl
import stockmonitoringbot.messengerservices.TelegramService
import stockmonitoringbot.stockpriceservices.{AlphavantageHttpRequestExecutor, AlphavantageStockPriceServiceComponent}
import stockmonitoringbot.stocksandratescache.StocksAndExchangeRatesCacheImpl

/**
  * Created by amir.
  */
object Main extends App {

  val bot = new StockMonitoringBot
    with InMemoryUserDataStorageComponentImpl
    with StocksAndExchangeRatesCacheImpl
    with AlphavantageStockPriceServiceComponent
    with AlphavantageHttpRequestExecutor
    with TelegramService
    with ExecutionContextImpl
    with ActorSystemComponentImpl
    with ApiKeysImpl
  bot.run()

}

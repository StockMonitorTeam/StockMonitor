package stockmonitoringbot

import stockmonitoringbot.datastorage.InMemoryUserDataStorage
import stockmonitoringbot.messengerservices.TelegramService
import stockmonitoringbot.notificationhandlers.{DailyNotificationHandlerComponentImpl, TriggerNotificationHandlerComponentImpl}
import stockmonitoringbot.stockpriceservices.{AlphavantageHttpRequestExecutor, AlphavantageStockPriceService}
import stockmonitoringbot.stocksandratescache.StocksAndExchangeRatesCacheImpl

/**
  * Created by amir.
  */
object Main extends App {

  val bot = new {}
    with DailyNotificationHandlerComponentImpl
    with TriggerNotificationHandlerComponentImpl
    with InMemoryUserDataStorage
    with StocksAndExchangeRatesCacheImpl
    with AlphavantageStockPriceService
    with AlphavantageHttpRequestExecutor
    with TelegramService
    with ExecutionContextImpl
    with ActorSystemComponentImpl
    with ApiKeysImpl
  bot.run()

}

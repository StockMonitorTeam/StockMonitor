package stockmonitoringbot

import stockmonitoringbot.datastorage.InMemoryUserDataStorageComponentImpl
import stockmonitoringbot.messengerservices.TelegramService
import stockmonitoringbot.notificationhandlers.{DailyNotificationHandlerComponentImpl, TriggerNotificationHandlerComponentImpl}
import stockmonitoringbot.stockpriceservices.{AlphavantageHttpRequestExecutor, AlphavantageStockPriceServiceComponent}
import stockmonitoringbot.stocksandratescache.PriceCacheComponentImpl

/**
  * Created by amir.
  */
object Main extends App {

  val bot = new DailyNotificationHandlerComponentImpl
    with TriggerNotificationHandlerComponentImpl
    with InMemoryUserDataStorageComponentImpl
    with PriceCacheComponentImpl
    with AlphavantageStockPriceServiceComponent
    with AlphavantageHttpRequestExecutor
    with TelegramService
    with ExecutionContextImpl
    with ActorSystemComponentImpl
    with ApiKeysImpl
  bot.run()

}

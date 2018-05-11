package stockmonitoringbot

import stockmonitoringbot.datastorage.postgresdb.{PostgresDBComponent, PostgresDBConnectionComponentImpl}
import stockmonitoringbot.messengerservices.TelegramMessageReceiverAndSenderComponent
import stockmonitoringbot.notificationhandlers.{DailyNotificationHandlerComponentImpl, TriggerNotificationHandlerComponentImpl}
import stockmonitoringbot.stockpriceservices.{AlphavantageHttpRequestExecutor, AlphavantageStockPriceServiceComponent}
import stockmonitoringbot.stocksandratescache.PriceCacheComponentImpl

/**
  * Created by amir.
  */
object Main extends App {

  val bot = new DailyNotificationHandlerComponentImpl
    with TriggerNotificationHandlerComponentImpl
    with PostgresDBComponent
    with PostgresDBConnectionComponentImpl
    with PriceCacheComponentImpl
    with AlphavantageStockPriceServiceComponent
    with AlphavantageHttpRequestExecutor
    with TelegramMessageReceiverAndSenderComponent
    with ExecutionContextImpl
    with ActorSystemComponentImpl
    with AppConfigImpl {
    def start() =
      userDataStorage.initDB().map { _ =>
        messageReceiver.startReceiving()
        triggerNotificationHandler.start()
      }
  }
  bot.start()
}

package stockmonitoringbot

import stockmonitoringbot.datastorage.postgresdb.{PostgresDBComponent, PostgresDBConnectionComponentImpl}
import stockmonitoringbot.messengerservices.{UserActorServiceComponentImpl, TelegramMessageReceiverAndSenderComponent}
import stockmonitoringbot.notificationhandlers.{DailyNotificationHandlerComponentImpl, TriggerNotificationHandlerComponentImpl}
import stockmonitoringbot.stockpriceservices.{AlphavantageHttpRequestExecutor, AlphavantageStockPriceServiceComponent}
import stockmonitoringbot.stocksandratescache.PriceCacheComponentImpl

import scala.concurrent.Future

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
    with UserActorServiceComponentImpl
    with ExecutionContextImpl
    with ActorSystemComponentImpl
    with AppConfigImpl {
    def start(): Future[Unit] = for {
      _ <- userDataStorage.initDB()
      _ <- dailyNotificationHandler.init().zip(messageReceiver.startReceiving())
      _ = triggerNotificationHandler.start()
    } yield ()
  }
  bot.start()
}

package stockmonitoringbot

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import stockmonitoringbot.datastorage.DataStorageWithStockInfoLoad
import stockmonitoringbot.messengerservices.TelegramService
import stockmonitoringbot.stockpriceservices.AlphavantageStockPriceService

import scala.concurrent.ExecutionContext

/**
  * Created by amir.
  */
object Main extends App {

  val bot = new {
    implicit val system: ActorSystem = ActorSystem("ActorSystem")
    implicit val executionContext: ExecutionContext = system.dispatcher
    implicit val materializer: ActorMaterializer = ActorMaterializer()
  } with StockMonitoringBot with DataStorageWithStockInfoLoad with AlphavantageStockPriceService with TelegramService

  bot.run()

}

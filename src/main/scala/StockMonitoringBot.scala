import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory

import scala.io.StdIn
import scala.util.{Failure, Success}

/**
  * Created by amir.
  */
object StockMonitoringBot extends App {

  val config = ConfigFactory.load()
  val alphavantageAPIKey = config.getString("StockMonitor.Alphavantage.apikey")
  implicit val system: ActorSystem = ActorSystem("ActorSystem")
  import system.dispatcher

  val stockCostService = new AlphavantageStockPriceService(alphavantageAPIKey)

  while (true) {
    StdIn.readLine() match {
      case "exit" =>
        system.terminate().onComplete(_ => sys.exit())
      case stockName => //for example "MSFT", "BAC", "NFLX"
        println(s"started retrieving info about $stockName")
        stockCostService.getCost(stockName).onComplete({
          case Success(info) =>
            println(info)
          case Failure(exception) =>
            println(s"Can't get info about $stockName: $exception")
        })
    }
  }
}

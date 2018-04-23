package stockmonitoringbot.stockpriceservices

import java.time.ZonedDateTime

/**
  * Created by amir.
  */
sealed trait StockInfo {
  val name: String
  val price: Double
  val lastRefreshed: ZonedDateTime
}

case class DetailedStockInfo(name: String,
                             open: Double,
                             high: Double,
                             low: Double,
                             close: Double,
                             volume: Int,
                             lastRefreshed: ZonedDateTime) extends StockInfo {
  val price: Double = (high + low) / 2 //todo correct formula for stock price
}

case class BaseStockInfo(name: String,
                         price: Double,
                         valume: Int,
                         lastRefreshed: ZonedDateTime) extends StockInfo
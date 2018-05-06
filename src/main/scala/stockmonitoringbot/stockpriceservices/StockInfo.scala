package stockmonitoringbot.stockpriceservices

import java.time.ZonedDateTime

/**
  * Created by amir.
  */
sealed trait StockInfo {
  val name: String
  val price: BigDecimal
  val lastRefreshed: ZonedDateTime
}

case class DetailedStockInfo(name: String,
                             open: BigDecimal,
                             high: BigDecimal,
                             low: BigDecimal,
                             close: BigDecimal,
                             volume: Option[Long],
                             lastRefreshed: ZonedDateTime) extends StockInfo {
  val price: BigDecimal = close
}

case class BaseStockInfo(name: String,
                         price: BigDecimal,
                         volume: Option[Long],
                         lastRefreshed: ZonedDateTime) extends StockInfo

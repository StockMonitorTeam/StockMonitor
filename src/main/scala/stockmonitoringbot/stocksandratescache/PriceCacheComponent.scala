package stockmonitoringbot.stocksandratescache

import stockmonitoringbot.stockpriceservices.models.{CurrencyExchangeRateInfo, StockInfo}

import scala.concurrent.Future

/**
  * Created by amir.
  */

trait PriceCacheComponent {
  val priceCache: PriceCache
}

trait PriceCache {
  def getStockInfo(stock: String): Future[StockInfo]
  def contains(stock: String): Boolean

  def getExchangeRate(from: String, to: String): Future[CurrencyExchangeRateInfo]
  def contains(exchangePair: (String, String)): Boolean

  def updateCache(): Future[(Int, Int)]
}

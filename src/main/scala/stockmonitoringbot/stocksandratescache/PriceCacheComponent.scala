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
  def setStockInfo(stockInfo: StockInfo): Unit
  def getStocks: Set[String]
  def contains(stock: String): Boolean

  def getExchangeRate(from: String, to: String): Future[CurrencyExchangeRateInfo]
  def setExchangeRate(exchangeRate: CurrencyExchangeRateInfo): Unit
  def getExchangePairs: Set[(String, String)]
  def contains(exchangePair: (String, String)): Boolean

  def copy(): PriceCache
}

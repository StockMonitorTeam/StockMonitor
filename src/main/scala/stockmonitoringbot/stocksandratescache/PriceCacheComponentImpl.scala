package stockmonitoringbot.stocksandratescache

import java.util.concurrent.ConcurrentHashMap

import stockmonitoringbot.ExecutionContextComponent
import stockmonitoringbot.stockpriceservices.{CurrencyExchangeRateInfo, StockInfo, StockPriceServiceComponent}

import scala.collection.JavaConverters._
import scala.concurrent.Future

/**
  * Created by amir.
  */

trait PriceCacheComponentImpl extends PriceCacheComponent {
  this: StockPriceServiceComponent //todo initial load stocks in cache
    with ExecutionContextComponent =>

  override val priceCache = new PriceCacheImpl

  class PriceCacheImpl extends PriceCache {

    private val stocks: ConcurrentHashMap[String, StockInfo] = new ConcurrentHashMap()
    private val exchangeRates: ConcurrentHashMap[(String, String), CurrencyExchangeRateInfo] = new ConcurrentHashMap()

    override def getStockInfo(stock: String): Future[StockInfo] =
      if (stocks.containsKey(stock))
        Future.successful(stocks.get(stock))
      else
        stockPriceService.getStockPriceInfo(stock).map { info =>
          setStockInfo(info)
          info
        }

    override def setStockInfo(stockInfo: StockInfo): Unit = {
      stocks.put(stockInfo.name, stockInfo)
      ()
    }

    override def getStocks: Set[String] =
      stocks.keySet().asScala.toSet

    override def contains(stock: String): Boolean =
      stocks.containsKey(stock)

    override def getExchangeRate(from: String, to: String): Future[CurrencyExchangeRateInfo] =
      if (exchangeRates.containsKey((from, to)))
        Future.successful(exchangeRates.get((from, to)))
      else
        stockPriceService.getCurrencyExchangeRate(from, to).map { info =>
          setExchangeRate(info)
          info
        }

    override def setExchangeRate(exchangeRate: CurrencyExchangeRateInfo): Unit = {
      exchangeRates.put((exchangeRate.from, exchangeRate.to), exchangeRate)
      ()
    }

    override def getExchangePairs: Set[(String, String)] =
      exchangeRates.keySet().asScala.toSet

    override def contains(exchangePair: (String, String)): Boolean =
      exchangeRates.containsKey(exchangePair)

    override def copy(): PriceCacheImpl = {
      val cache = new PriceCacheImpl()
      stocks.forEach((_, stockInfo) => cache.setStockInfo(stockInfo))
      exchangeRates.forEach((_, rateInfo) => cache.setExchangeRate(rateInfo))
      cache
    }
  }
}
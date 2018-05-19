package stockmonitoringbot.stocksandratescache

import java.util.concurrent.ConcurrentHashMap

import stockmonitoringbot.ExecutionContextComponent
import stockmonitoringbot.stockpriceservices.StockPriceServiceComponent
import stockmonitoringbot.stockpriceservices.models.{CurrencyExchangeRateInfo, StockInfo}

import scala.collection.JavaConverters._
import scala.concurrent.Future

/**
  * Created by amir.
  */

trait PriceCacheComponentImpl extends PriceCacheComponent {
  this: ExecutionContextComponent
    with StockPriceServiceComponent =>

  override val priceCache = new PriceCacheImpl

  class PriceCacheImpl extends PriceCache {

    private val stocks: ConcurrentHashMap[String, StockInfo] = new ConcurrentHashMap()
    private val exchangeRates: ConcurrentHashMap[(String, String), CurrencyExchangeRateInfo] = new ConcurrentHashMap()

    override def getStockInfo(stock: String): Future[StockInfo] =
      Option(stocks.get(stock)).fold {
        stockPriceService.getStockPriceInfo(stock).map { info =>
          priceCache.setStockInfo(info)
          info
        }
      }(Future.successful)

    override def contains(stock: String): Boolean =
      stocks.containsKey(stock)

    override def getExchangeRate(from: String, to: String): Future[CurrencyExchangeRateInfo] =
      Option(exchangeRates.get((from, to))).fold {
        stockPriceService.getCurrencyExchangeRate(from, to).map { info =>
          priceCache.setExchangeRate(info)
          info
        }
      }(Future.successful)

    override def contains(exchangePair: (String, String)): Boolean =
      exchangeRates.containsKey(exchangePair)

    private def setStockInfo(stockInfo: StockInfo): Unit = {
      stocks.put(stockInfo.name, stockInfo)
      ()
    }

    private def setExchangeRate(exchangeRate: CurrencyExchangeRateInfo): Unit = {
      exchangeRates.put((exchangeRate.from, exchangeRate.to), exchangeRate)
      ()
    }

    /**
      *
      * @return number of updated stocks & exchange rates
      */
    override def updateCache(): Future[(Int, Int)] = {
      val stockUpdate: Future[Int] = stockPriceService.getBatchPrices(stocks.keySet().asScala.toSeq).map { updatedStocks =>
        updatedStocks.foreach(setStockInfo)
        updatedStocks.size
      }
      val ratesUpdate: Future[Int] = Future.traverse(exchangeRates.keySet().asScala.toSeq) { pair =>
        stockPriceService.getCurrencyExchangeRate(pair._1, pair._2).map(setExchangeRate)
      }.map(_.size)
      for {numOfUpdatedStocks <- stockUpdate
           numOfUpdatedRates <- ratesUpdate
      } yield (numOfUpdatedStocks, numOfUpdatedRates)
    }

  }
}
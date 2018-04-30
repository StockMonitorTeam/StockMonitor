package stockmonitoringbot.stockpriceservices

import scala.concurrent.Future

/**
  * Created by amir.
  */
trait StockPriceServiceComponent {
  val stockPriceService: StockPriceService
}

trait StockPriceService {
  def getStockPriceInfo(stockName: String): Future[StockInfo]
  def getBatchPrices(stocks: Seq[String]): Future[Seq[StockInfo]]
  def getCurrencyExchangeRate(from: String, to: String): Future[CurrencyExchangeRateInfo]
}
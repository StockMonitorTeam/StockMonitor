package stockmonitoringbot.stockpriceservices

import scala.concurrent.Future

/**
  * Created by amir.
  */
trait StockPriceService {
  def getStockPriceInfo(stockName: String): Future[StockInfo]
}
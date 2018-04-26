package stockmonitoringbot.datastorage

import stockmonitoringbot.ExecutionContextComponent
import stockmonitoringbot.stockpriceservices.StockPriceService

import scala.concurrent.Future

case class PriceInfo(ticker: String, org: String, price: Double)

/**
  * Created by amir.
  */
trait DataStorageWithStockInfoLoad extends InMemoryDataStorage {
  self: ExecutionContextComponent with StockPriceService =>

  override def getPrice(stock: String): Future[Double] =
    containsStock(stock).flatMap { contains =>
      if (contains)
        super.getPrice(stock)
      else
        for {stockInfo <- getStockPriceInfo(stock)
             _ <- addStock(stock, stockInfo.price)
        } yield stockInfo.price
    }

  override def addNotification(notification: Notification): Future[Unit] =
    containsStock(notification.stock).flatMap { contains =>
      if (contains)
        super.addNotification(notification)
      else
        for {stockInfo <- getStockPriceInfo(notification.stock)
             _ <- addStock(notification.stock, stockInfo.price)
             _ <- super.addNotification(notification)
        } yield ()
    }

}

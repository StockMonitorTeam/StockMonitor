package stockmonitoringbot.datastorage

import stockmonitoringbot.stockpriceservices.StockPriceService

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by amir.
  */
trait DataStorageWithStockInfoLoad extends DataStorage {
  self: StockPriceService =>

  implicit val executionContext: ExecutionContext
  private val inMemoryNotificationService = new InMemoryDataStorage()

  def addStock(stock: String, price: Double): Future[Unit] =
    inMemoryNotificationService.addStock(stock, price)

  override def updateStockPrice(stock: String, newPrice: Double): Future[Seq[Notification]] =
    inMemoryNotificationService.updateStockPrice(stock, newPrice)

  override def getPrice(stock: String): Future[Double] =
    inMemoryNotificationService.containsStock(stock).flatMap { contains =>
      if (contains)
        inMemoryNotificationService.getPrice(stock)
      else
        for {stockInfo <- getStockPriceInfo(stock)
             _ <- inMemoryNotificationService.addStock(stock, stockInfo.price)
        } yield stockInfo.price
    }

  override def addNotification(notification: Notification): Future[Unit] =
    inMemoryNotificationService.containsStock(notification.stock).flatMap { contains =>
      if (contains)
        inMemoryNotificationService.addNotification(notification)
      else
        for {stockInfo <- getStockPriceInfo(notification.stock)
             _ <- inMemoryNotificationService.addStock(notification.stock, stockInfo.price)
             _ <- inMemoryNotificationService.addNotification(notification)
        } yield ()
    }

  override def deleteNotification(notification: Notification): Future[Unit] =
    inMemoryNotificationService.deleteNotification(notification)

  override def getNotifications(userId: Long): Future[Seq[Notification]] =
    inMemoryNotificationService.getNotifications(userId)

  override def getStocks: Future[Set[String]] =
    inMemoryNotificationService.getStocks
}

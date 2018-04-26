package stockmonitoringbot.datastorage

import scala.concurrent.Future


/**
  * Created by amir.
  */
trait DataStorage {

  /**
    * @param stock
    * @param newPrice
    * @return Returns triggered notifications
    */
  def updateStockPrice(stock: String, newPrice: Double): Future[Seq[Notification]]

  def addStock(stock: String, price: Double): Future[Unit]

  def getPrice(stock: String): Future[Double]

  def getStocks: Future[Set[String]]

  def addNotification(notification: Notification): Future[Unit]

  def deleteNotification(notification: Notification): Future[Unit]

  def getNotifications(userId: Long): Future[Seq[Notification]]

  def getPortfolios(userId: Long): Future[Seq[Portfolio]]

  def isPortfoliosExist(userId: Long): Future[Boolean]

}

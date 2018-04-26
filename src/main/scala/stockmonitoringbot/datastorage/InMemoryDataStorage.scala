package stockmonitoringbot.datastorage

import stockmonitoringbot.ExecutionContextComponent

import scala.collection.mutable
import scala.concurrent.Future

/**
  * Created by amir.
  */
trait InMemoryDataStorage extends DataStorage {
  self: ExecutionContextComponent =>
  private val stockCurrentPrice: mutable.Map[String, Double] = mutable.HashMap.empty
  private val stockNotifications: mutable.Map[String, mutable.TreeSet[Notification]] = mutable.Map.empty
  private val userNotifications: mutable.Map[Long, mutable.HashSet[Notification]] = mutable.Map.empty
  private val userPortfolio: mutable.Map[Long, mutable.HashSet[Portfolio]] = mutable.Map.empty

  override def addStock(stock: String, price: Double): Future[Unit] = Future(synchronized {
    stockCurrentPrice += stock -> price
    stockNotifications += stock -> mutable.TreeSet.empty
    ()
  })

  override def updateStockPrice(stock: String, newPrice: Double): Future[Seq[Notification]] = Future(synchronized {
    if (!stockNotifications.contains(stock))
      throw new IllegalArgumentException(s"stock $stock is not found")
    val oldPrice = stockCurrentPrice(stock)
    stockCurrentPrice(stock) = newPrice
    val triggered = stockNotifications(stock).rangeImpl(
      Some(Notification(Math.min(oldPrice, newPrice))),
      Some(Notification(Math.max(oldPrice, newPrice)))).toSeq
    triggered.foreach { notification =>
      stockNotifications(stock) -= notification
      userNotifications(notification.userId) -= notification
    }
    triggered
  })

  override def getPrice(stock: String): Future[Double] = Future(synchronized {
    stockCurrentPrice.getOrElse(stock, throw new IllegalArgumentException(s"stock $stock is not found"))
  })

  override def addNotification(notification: Notification): Future[Unit] = Future(synchronized {
    val stock = notification.stock
    if (!stockNotifications.contains(stock))
      throw new IllegalArgumentException(s"stock $stock is not found")
    notification.notificationType match {
      case RaiseNotification if notification.price <= stockCurrentPrice(stock) =>
        throw new IllegalArgumentException("stock price is already higher than notification price")
      case FallNotification if notification.price >= stockCurrentPrice(stock) =>
        throw new IllegalArgumentException("stock price is already lower than notification price")
      case _ =>
    }
    stockNotifications.getOrElseUpdate(stock, mutable.TreeSet.empty).add(notification)
    userNotifications.getOrElseUpdate(notification.userId, mutable.HashSet.empty).add(notification)
    ()
  })

  override def deleteNotification(notification: Notification): Future[Unit] = Future(synchronized {
    if (!stockNotifications.contains(notification.stock))
      throw new IllegalArgumentException(s"stock ${notification.stock} is not found")
    stockNotifications(notification.stock) -= notification
    userNotifications(notification.userId) -= notification
    ()
  })

  override def getNotifications(userId: Long): Future[Seq[Notification]] = Future(synchronized {
    userNotifications.getOrElse(userId, Seq()).toSeq
  })

  override def getStocks: Future[Set[String]] = Future(synchronized {
    stockCurrentPrice.keySet.toSet
  })

  override def getPortfolios(userId: Long): Future[Seq[Portfolio]] = Future(synchronized {
    userPortfolio.getOrElse(userId, Seq()).toSeq
  })

  override def isPortfoliosExist(userId: Long): Future[Boolean] = getPortfolios(userId) map (x => x.nonEmpty)

  def containsStock(stock: String): Future[Boolean] = Future(synchronized {
    stockCurrentPrice.contains(stock)
  })
}
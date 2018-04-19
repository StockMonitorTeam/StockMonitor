package stockmonitoringbot.datastorage

import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}


/**
  * Created by amir.
  */
class InMemoryDataStorageTest extends FlatSpec with Matchers {

  val stock = "MSFT"

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  "InMemoryDataStorage" should "return stock price" in {
    val notificationService = new InMemoryDataStorage()
    val test = for {_ <- notificationService.addStock(stock, 23)
                    firstPrice <- notificationService.getPrice(stock)
                    _ <- notificationService.updateStockPrice(stock, 25)
                    secondPrice <- notificationService.getPrice(stock)
    } yield {
      firstPrice shouldBe 23
      secondPrice shouldBe 25
      ()
    }
    Await.ready(test, Duration.Inf)
  }

  "InMemoryDataStorage" should "return triggered notifications when price raise" in {
    val notificationService = new InMemoryDataStorage()
    val test = for {_ <- notificationService.addStock(stock, 23)
                    _ <- notificationService.addNotification(Notification(stock, 24, RaiseNotification, 0))
                    _ <- notificationService.addNotification(Notification(stock, 24, RaiseNotification, 1))
                    _ <- notificationService.addNotification(Notification(stock, 26, RaiseNotification, 0))
                    notifications <- notificationService.updateStockPrice(stock, 25)
    } yield {
      notifications should contain theSameElementsAs
        Seq(Notification(stock, 24, RaiseNotification, 0), Notification(stock, 24, RaiseNotification, 1))
      ()
    }
    Await.ready(test, Duration.Inf)
  }

  "InMemoryDataStorage" should "return triggered notifications when price fall" in {
    val notificationService = new InMemoryDataStorage()
    val test = for {_ <- notificationService.addStock(stock, 23)
                    _ <- notificationService.addNotification(Notification(stock, 22, RaiseNotification, 0))
                    _ <- notificationService.addNotification(Notification(stock, 22, RaiseNotification, 1))
                    _ <- notificationService.addNotification(Notification(stock, 19, RaiseNotification, 0))
                    notifications <- notificationService.updateStockPrice(stock, 21)
    } yield {
      notifications should contain theSameElementsAs
        Seq(Notification(stock, 22, RaiseNotification, 0), Notification(stock, 22, RaiseNotification, 1))
      ()
    }
    Await.ready(test, Duration.Inf)
  }

  "InMemoryDataStorage" should "store user's notifications" in {
    val notificationService = new InMemoryDataStorage()
    val test = for {_ <- notificationService.addStock(stock, 23)
                    _ <- notificationService.addNotification(Notification(stock, 22, RaiseNotification, 0))
                    _ <- notificationService.addNotification(Notification(stock, 23, RaiseNotification, 0))
                    _ <- notificationService.addNotification(Notification(stock, 24, RaiseNotification, 0))
                    _ <- notificationService.deleteNotification(Notification(stock, 23, RaiseNotification, 0))
                    notifications <- notificationService.getNotifications(0)
    } yield {
      notifications should contain theSameElementsAs
        Seq(Notification(stock, 22, RaiseNotification, 0), Notification(stock, 24, RaiseNotification, 0))
      ()
    }
    Await.ready(test, Duration.Inf)
  }


}

package stockmonitoringbot.datastorage

import java.time.LocalTime

import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}
import stockmonitoringbot.ExecutionContextComponent
import stockmonitoringbot.datastorage.models._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.language.postfixOps

/**
  * Created by amir.
  */
class InMemoryDataStorageTest extends FlatSpec with Matchers with BeforeAndAfter {

  val stock = "MSFT"

  implicit val executionContextGlobal: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global

  trait ExecutionContextGlobal extends ExecutionContextComponent {
    override implicit val executionContext: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global
  }

  var storage: UserDataStorage = _

  before {
    storage = new {}
      with InMemoryUserDataStorage
      with ExecutionContextGlobal

  }

  "InMemoryUserDataStorage" should "store daily notifications" in {
    val dailyNotification1 = PortfolioDailyNotification(0, "newPortfolio", LocalTime.of(0, 0))
    val dailyNotification2 = PortfolioDailyNotification(0, "newPortfolio2", LocalTime.of(0, 0))
    val test = for {_ <- storage.addDailyNotification(dailyNotification1)
                    _ <- storage.addDailyNotification(dailyNotification2)
                    notifications <- storage.getUsersDailyNotifications(0)
                    _ = notifications should contain theSameElementsAs Seq(dailyNotification1, dailyNotification2)
                    _ <- storage.deleteDailyNotification(dailyNotification1)
                    notifications <- storage.getUsersDailyNotifications(0)
                    _ = notifications should contain theSameElementsAs Seq(dailyNotification2)
    } yield
      ()
    Await.ready(test, 1 second)
  }

  "InMemoryUserDataStorage" should "store trigger notifications" in {
    val triggerNotification1 = PortfolioTriggerNotification(0, "newPortfolio", 40, RaiseNotification)
    val triggerNotification2 = PortfolioTriggerNotification(0, "newPortfolio2", 40, RaiseNotification)
    val test = for {_ <- storage.addTriggerNotification(triggerNotification1)
                    _ <- storage.addTriggerNotification(triggerNotification2)
                    notifications <- storage.getUsersTriggerNotifications(0)
                    _ = notifications should contain theSameElementsAs Seq(triggerNotification1, triggerNotification2)
                    _ <- storage.deleteTriggerNotification(triggerNotification1)
                    notifications <- storage.getUsersTriggerNotifications(0)
                    _ = notifications should contain theSameElementsAs Seq(triggerNotification2)
    } yield
      ()
    Await.ready(test, 1 second)
  }

  "InMemoryUserDataStorage" should "be able to return all trigger notifications" in {
    val triggerNotification1 = PortfolioTriggerNotification(0, "newPortfolio", 40, RaiseNotification)
    val triggerNotification2 = PortfolioTriggerNotification(1, "newPortfolio2", 40, RaiseNotification)
    val test = for {_ <- storage.addTriggerNotification(triggerNotification1)
                    _ <- storage.addTriggerNotification(triggerNotification2)
                    notifications <- storage.getAllTriggerNotifications
                    _ = notifications should contain theSameElementsAs Seq(triggerNotification1, triggerNotification2)
    } yield
      ()
    Await.ready(test, 1 second)
  }

  "InMemoryUserDataStorage" should "store users portfolios" in {
    val portfolio1 = Portfolio(0, "newPortfolio", USD, Map("MSFT" -> 23, "AAPL" -> 42))
    val portfolio2 = Portfolio(0, "newPortfolio2", USD, Map("MSFT" -> 23))
    val test = for {_ <- storage.addPortfolio(portfolio1)
                    _ <- storage.addPortfolio(portfolio2)
                    portfolios <- storage.getUserPortfolios(0)
                    _ = portfolios should contain theSameElementsAs Seq(portfolio1, portfolio2)
                    _ <- storage.deletePortfolio(0, "newPortfolio")
                    portfolios <- storage.getUserPortfolios(0)
                    _ = portfolios should contain theSameElementsAs Seq(portfolio2)
    } yield
      ()
    Await.ready(test, 1 second)
  }

  "InMemoryUserDataStorage" should "add/delete stocks from portfolios" in {
    val portfolio1 = Portfolio(0, "newPortfolio", USD, Map("MSFT" -> 23, "AAPL" -> 42))
    val portfolio2 = Portfolio(0, "newPortfolio", USD, Map("MSFT" -> 23, "AAPL" -> 42, "YNDX" -> 1))
    val test = for {_ <- storage.addPortfolio(portfolio1)
                    _ <- storage.addStockToPortfolio(0, "newPortfolio", "YNDX", 1)
                    portfolio <- storage.getPortfolio(0, "newPortfolio")
                    _ = portfolio shouldBe portfolio2
                    _ <- storage.deleteStockFromPortfolio(0, "newPortfolio", "YNDX")
                    portfolio <- storage.getPortfolio(0, "newPortfolio")
                    _ = portfolio shouldBe portfolio1
    } yield
      ()
    Await.ready(test, 1 second)
  }

}

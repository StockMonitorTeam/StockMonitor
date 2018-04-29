package stockmonitoringbot.datastorage

import java.time.LocalTime

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}
import stockmonitoringbot.ExecutionContextComponent
import stockmonitoringbot.datastorage.models._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.language.postfixOps

/**
  * Created by amir.
  */
class InMemoryDataStorageTest extends FlatSpec with Matchers with ScalaFutures {

  val stock = "MSFT"

  implicit val executionContextGlobal: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global

  trait TestWiring {
    val objWithDataStorage = new InMemoryUserDataStorageComponentImpl
      with ExecutionContextComponent {
      override val executionContext = scala.concurrent.ExecutionContext.global
    }
  }

  "InMemoryUserDataStorage" should "store daily notifications" in new TestWiring {
    val dailyNotification1 = PortfolioDailyNotification(0, "newPortfolio", LocalTime.of(0, 0))
    val dailyNotification2 = PortfolioDailyNotification(0, "newPortfolio2", LocalTime.of(0, 0))
    val test = for {_ <- objWithDataStorage.userDataStorage.addDailyNotification(dailyNotification1)
                    _ <- objWithDataStorage.userDataStorage.addDailyNotification(dailyNotification2)
                    notifications <- objWithDataStorage.userDataStorage.getUsersDailyNotifications(0)
                    _ = notifications should contain theSameElementsAs Seq(dailyNotification1, dailyNotification2)
                    _ <- objWithDataStorage.userDataStorage.deleteDailyNotification(dailyNotification1)
                    notifications <- objWithDataStorage.userDataStorage.getUsersDailyNotifications(0)
                    _ = notifications should contain theSameElementsAs Seq(dailyNotification2)
    } yield
      ()
    test.futureValue shouldBe (())
  }

  "InMemoryUserDataStorage" should "store trigger notifications" in new TestWiring {
    val triggerNotification1 = PortfolioTriggerNotification(0, "newPortfolio", 40, RaiseNotification)
    val triggerNotification2 = PortfolioTriggerNotification(0, "newPortfolio2", 40, RaiseNotification)
    val test = for {_ <- objWithDataStorage.userDataStorage.addTriggerNotification(triggerNotification1)
                    _ <- objWithDataStorage.userDataStorage.addTriggerNotification(triggerNotification2)
                    notifications <- objWithDataStorage.userDataStorage.getUsersTriggerNotifications(0)
                    _ = notifications should contain theSameElementsAs Seq(triggerNotification1, triggerNotification2)
                    _ <- objWithDataStorage.userDataStorage.deleteTriggerNotification(triggerNotification1)
                    notifications <- objWithDataStorage.userDataStorage.getUsersTriggerNotifications(0)
                    _ = notifications should contain theSameElementsAs Seq(triggerNotification2)
    } yield
      ()
    test.futureValue shouldBe (())
  }

  "InMemoryUserDataStorage" should "be able to return all trigger notifications" in new TestWiring {
    val triggerNotification1 = PortfolioTriggerNotification(0, "newPortfolio", 40, RaiseNotification)
    val triggerNotification2 = PortfolioTriggerNotification(1, "newPortfolio2", 40, RaiseNotification)
    val test = for {_ <- objWithDataStorage.userDataStorage.addTriggerNotification(triggerNotification1)
                    _ <- objWithDataStorage.userDataStorage.addTriggerNotification(triggerNotification2)
                    notifications <- objWithDataStorage.userDataStorage.getAllTriggerNotifications
                    _ = notifications should contain theSameElementsAs Seq(triggerNotification1, triggerNotification2)
    } yield
      ()
    test.futureValue shouldBe (())
  }

  "InMemoryUserDataStorage" should "store users portfolios" in new TestWiring {
    val portfolio1 = Portfolio(0, "newPortfolio", USD, Map("MSFT" -> 23, "AAPL" -> 42))
    val portfolio2 = Portfolio(0, "newPortfolio2", USD, Map("MSFT" -> 23))
    val test = for {_ <- objWithDataStorage.userDataStorage.addPortfolio(portfolio1)
                    _ <- objWithDataStorage.userDataStorage.addPortfolio(portfolio2)
                    portfolios <- objWithDataStorage.userDataStorage.getUsersPortfolios(0)
                    _ = portfolios should contain theSameElementsAs Seq(portfolio1, portfolio2)
                    _ <- objWithDataStorage.userDataStorage.deletePortfolio(0, "newPortfolio")
                    portfolios <- objWithDataStorage.userDataStorage.getUsersPortfolios(0)
                    _ = portfolios should contain theSameElementsAs Seq(portfolio2)
    } yield
      ()
    test.futureValue shouldBe (())
  }

  "InMemoryUserDataStorage" should "add/delete stocks from portfolios" in new TestWiring {
    val portfolio1 = Portfolio(0, "newPortfolio", USD, Map("MSFT" -> 23, "AAPL" -> 42))
    val portfolio2 = Portfolio(0, "newPortfolio", USD, Map("MSFT" -> 23, "AAPL" -> 42, "YNDX" -> 1))
    val test = for {_ <- objWithDataStorage.userDataStorage.addPortfolio(portfolio1)
                    _ <- objWithDataStorage.userDataStorage.addStockToPortfolio(0, "newPortfolio", "YNDX", 1)
                    portfolio <- objWithDataStorage.userDataStorage.getPortfolio(0, "newPortfolio")
                    _ = portfolio shouldBe portfolio2
                    _ <- objWithDataStorage.userDataStorage.deleteStockFromPortfolio(0, "newPortfolio", "YNDX")
                    portfolio <- objWithDataStorage.userDataStorage.getPortfolio(0, "newPortfolio")
                    _ = portfolio shouldBe portfolio1
    } yield
      ()
    test.futureValue shouldBe (())
  }

  "InMemoryUserDataStorage" should "delete notifications when deleting portfolio" in new TestWiring {
    val portfolio1 = Portfolio(0, "newPortfolio", USD, Map("MSFT" -> 23, "AAPL" -> 42))
    val triggerNotification1 = PortfolioTriggerNotification(0, "newPortfolio", 40, RaiseNotification)
    val triggerNotification2 = PortfolioTriggerNotification(0, "newPortfolio1", 40, RaiseNotification)
    val dailyNotification1 = PortfolioDailyNotification(0, "newPortfolio", LocalTime.of(0, 0))
    val dailyNotification2 = PortfolioDailyNotification(0, "newPortfolio1", LocalTime.of(0, 0))
    val test = for {_ <- objWithDataStorage.userDataStorage.addPortfolio(portfolio1)
                    _ <- objWithDataStorage.userDataStorage.addDailyNotification(dailyNotification1)
                    _ <- objWithDataStorage.userDataStorage.addDailyNotification(dailyNotification2)
                    _ <- objWithDataStorage.userDataStorage.addTriggerNotification(triggerNotification1)
                    _ <- objWithDataStorage.userDataStorage.addTriggerNotification(triggerNotification2)
                    _ <- objWithDataStorage.userDataStorage.deletePortfolio(0, "newPortfolio")
                    dailyNotifications <- objWithDataStorage.userDataStorage.getUsersDailyNotifications(0)
                    _ = dailyNotifications should contain theSameElementsAs Seq(dailyNotification2)
                    triggerNotifications <- objWithDataStorage.userDataStorage.getUsersTriggerNotifications(0)
                    _ = triggerNotifications should contain theSameElementsAs Seq(triggerNotification2)
    } yield
      ()
    test.futureValue shouldBe (())
  }

}

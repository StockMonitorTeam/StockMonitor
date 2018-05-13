package stockmonitoringbot.datastorage

import java.time.{LocalTime, ZoneId}

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}
import stockmonitoringbot.datastorage.models._

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by amir.
  */
class InMemoryDataStorageTest extends FlatSpec with Matchers with ScalaFutures {

  val stock = "MSFT"

  implicit val executionContextGlobal: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global

  trait TestWiring extends InMemoryUserDataStorageComponentImpl {
    implicit val patienceConfig: PatienceConfig = PatienceConfig(500 millis, 20 millis)
  }

  "InMemoryUserDataStorage" should "store daily notifications" in new TestWiring {
    val dailyNotification1 = PortfolioDailyNotification(0, 0, "newPortfolio", LocalTime.of(0, 0))
    val dailyNotification2 = PortfolioDailyNotification(1, 0, "newPortfolio2", LocalTime.of(0, 0))
    val test = for {
      _ <- userDataStorage.addDailyNotification(dailyNotification1)
      _ <- userDataStorage.addDailyNotification(dailyNotification2)
      notifications <- userDataStorage.getUsersDailyNotifications(0)
      _ = notifications should contain theSameElementsAs Seq(dailyNotification1, dailyNotification2)
      _ <- userDataStorage.deleteDailyNotification(0)
      notifications <- userDataStorage.getUsersDailyNotifications(0)
      _ = notifications should contain theSameElementsAs Seq(dailyNotification2)
    } yield
      ()
    test.futureValue shouldBe (())
  }

  "InMemoryUserDataStorage" should "store trigger notifications" in new TestWiring {
    val triggerNotification1 = PortfolioTriggerNotification(0, 0, "newPortfolio", 40, Raise)
    val triggerNotification2 = PortfolioTriggerNotification(1, 0, "newPortfolio2", 40, Raise)
    val test = for {
      _ <- userDataStorage.addTriggerNotification(triggerNotification1)
      _ <- userDataStorage.addTriggerNotification(triggerNotification2)
      notifications <- userDataStorage.getUsersTriggerNotifications(0)
      _ = notifications should contain theSameElementsAs Seq(triggerNotification1, triggerNotification2)
      _ <- userDataStorage.deleteTriggerNotification(0)
      notifications <- userDataStorage.getUsersTriggerNotifications(0)
      _ = notifications should contain theSameElementsAs Seq(triggerNotification2)
    } yield
      ()
    test.futureValue shouldBe (())
  }

  "InMemoryUserDataStorage" should "be able to return all trigger notifications" in new TestWiring {
    val triggerNotification1 = PortfolioTriggerNotification(0, 0, "newPortfolio", 40, Raise)
    val triggerNotification2 = PortfolioTriggerNotification(1, 1, "newPortfolio2", 40, Raise)
    val test = for {
      _ <- userDataStorage.addTriggerNotification(triggerNotification1)
      _ <- userDataStorage.addTriggerNotification(triggerNotification2)
      notifications <- userDataStorage.getAllTriggerNotifications
      _ = notifications should contain theSameElementsAs Seq(triggerNotification1, triggerNotification2)
    } yield
      ()
    test.futureValue shouldBe (())
  }

  "InMemoryUserDataStorage" should "store users portfolios" in new TestWiring {
    val portfolio1 = Portfolio(0, 0, "newPortfolio", USD, Map("MSFT" -> 23, "AAPL" -> 42))
    val portfolio2 = Portfolio(0, 0, "newPortfolio2", USD, Map("MSFT" -> 23))
    val test = for {
      _ <- userDataStorage.addPortfolio(portfolio1)
      _ <- userDataStorage.addPortfolio(portfolio2)
      portfolios <- userDataStorage.getUserPortfolios(0)
      _ = portfolios should contain theSameElementsAs Seq(portfolio1, portfolio2)
      _ <- userDataStorage.deletePortfolio(0, "newPortfolio")
      portfolios <- userDataStorage.getUserPortfolios(0)
      _ = portfolios should contain theSameElementsAs Seq(portfolio2)
    } yield
      ()
    test.futureValue shouldBe (())
  }

  "InMemoryUserDataStorage" should "add/delete stocks from portfolios" in new TestWiring {
    val portfolio1 = Portfolio(0, 0, "newPortfolio", USD, Map("MSFT" -> 23, "AAPL" -> 42))
    val portfolio2 = Portfolio(0, 0, "newPortfolio", USD, Map("MSFT" -> 23, "AAPL" -> 42, "YNDX" -> 1))
    val test = for {
      _ <- userDataStorage.addPortfolio(portfolio1)
      _ <- userDataStorage.addStockToPortfolio(0, "newPortfolio", "YNDX", 1)
      portfolio <- userDataStorage.getPortfolio(0, "newPortfolio")
      _ = portfolio shouldBe portfolio2
      _ <- userDataStorage.deleteStockFromPortfolio(0, "newPortfolio", "YNDX")
      portfolio <- userDataStorage.getPortfolio(0, "newPortfolio")
      _ = portfolio shouldBe portfolio1
    } yield
      ()
    test.futureValue shouldBe (())
  }

  "InMemoryUserDataStorage" should "delete notifications when deleting portfolio" in new TestWiring {
    val portfolio1 = Portfolio(0, 0, "newPortfolio", USD, Map("MSFT" -> 23, "AAPL" -> 42))
    val triggerNotification1 = PortfolioTriggerNotification(0, 0, "newPortfolio", 40, Raise)
    val triggerNotification2 = PortfolioTriggerNotification(1, 0, "newPortfolio1", 40, Raise)
    val dailyNotification1 = PortfolioDailyNotification(0, 0, "newPortfolio", LocalTime.of(0, 0))
    val dailyNotification2 = PortfolioDailyNotification(1, 0, "newPortfolio1", LocalTime.of(0, 0))
    val test = for {
      _ <- userDataStorage.addPortfolio(portfolio1)
      _ <- userDataStorage.addDailyNotification(dailyNotification1)
      _ <- userDataStorage.addDailyNotification(dailyNotification2)
      _ <- userDataStorage.addTriggerNotification(triggerNotification1)
      _ <- userDataStorage.addTriggerNotification(triggerNotification2)
      _ <- userDataStorage.deletePortfolio(0, "newPortfolio")
      dailyNotifications <- userDataStorage.getUsersDailyNotifications(0)
      _ = dailyNotifications should contain theSameElementsAs Seq(dailyNotification2)
      triggerNotifications <- userDataStorage.getUsersTriggerNotifications(0)
      _ = triggerNotifications should contain theSameElementsAs Seq(triggerNotification2)
    } yield
      ()
    test.futureValue shouldBe (())
  }

  "InMemoryUserDataStorage" should "store users" in new TestWiring {
    val user1 = User(0, ZoneId.of("GMT-12"))
    val user2 = User(0, ZoneId.of("GMT+12"))
    val test = for {
      _ <- userDataStorage.setUser(user1)
      user <- userDataStorage.getUser(0)
      _ = user shouldBe Some(user1)
      _ <- userDataStorage.setUser(user2)
      user <- userDataStorage.getUser(0)
      _ = user shouldBe Some(user2)
      user <- userDataStorage.getUser(23)
      _ = user shouldBe None
    } yield
      ()
    test.futureValue shouldBe (())
  }

}

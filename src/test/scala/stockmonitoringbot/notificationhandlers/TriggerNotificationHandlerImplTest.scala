package stockmonitoringbot.notificationhandlers

import info.mukel.telegrambot4s.methods.SendMessage
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}
import stockmonitoringbot.datastorage.models._
import stockmonitoringbot.{ActorSystemComponentImpl, AppConfig, ExecutionContextImpl}
import stockmonitoringbot.datastorage.{UserDataStorage, UserDataStorageComponent}
import stockmonitoringbot.messengerservices.MessageSenderComponent
import stockmonitoringbot.stockpriceservices._
import stockmonitoringbot.stockpriceservices.models.{BaseStockInfo, CurrencyExchangeRateInfo}
import stockmonitoringbot.stocksandratescache.{PriceCache, PriceCacheComponent}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by amir.
  */
class TriggerNotificationHandlerImplTest extends FlatSpec with Matchers with ScalaFutures with MockFactory {

  /*
  StockPriceServiceComponent
  with UserDataStorageComponent
  with PriceCacheComponent
  with MessageSenderComponent
   */

  private trait TestWiring extends TriggerNotificationHandlerComponentImpl
    with MessageSenderComponent
    with UserDataStorageComponent
    with PriceCacheComponent
    with StockPriceServiceComponent
    with ActorSystemComponentImpl
    with ExecutionContextImpl
    with AppConfig {
    implicit val patienceConfig: PatienceConfig = PatienceConfig(500 millis, 20 millis)
    override val messageSender = mockFunction[SendMessage, Unit]
    override val priceCache = mock[PriceCache]
    override val userDataStorage = mock[UserDataStorage]
    override val stockPriceService = mock[StockPriceService]
    override def getKey(s: String) = ""
  }

  val time = parseZonedDateTime("2018-04-20 15:05:00", "US/Eastern")
  val si1 = BaseStockInfo("MSFT", 23, Some(0), time)
  val si2 = BaseStockInfo("AAPL", 32, Some(0), time)
  val eri = CurrencyExchangeRateInfo("USD", "", "RUB", "", 23, time)

  "TriggerNotificationHandler" should "send messages if stockTriggerNotification triggers" in new TestWiring {
    val notR = StockTriggerNotification(0, 0, si1.name, 22, Raise)
    val notF = StockTriggerNotification(1, 0, si1.name, 24, Fall)
    userDataStorage.getAllTriggerNotifications _ expects() returning Future.successful(Seq(notR, notF))
    userDataStorage.deleteTriggerNotification _ expects 0 returning Future.unit
    userDataStorage.deleteTriggerNotification _ expects 1 returning Future.unit
    priceCache.getStockInfo _ expects si1.name returning Future.successful(si1) atLeastOnce()
    messageSender expects * twice()
    triggerNotificationHandler.checkTriggers().futureValue shouldBe (())
  }

  "TriggerNotificationHandler" should "send messages if exchangeRateTriggerNotification triggers" in new TestWiring {
    val notR = ExchangeRateTriggerNotification(0, 0, (eri.from, eri.to), 22, Raise)
    val notF = ExchangeRateTriggerNotification(1, 0, (eri.from, eri.to), 24, Fall)
    userDataStorage.getAllTriggerNotifications _ expects() returning Future.successful(Seq(notR, notF))
    userDataStorage.deleteTriggerNotification _ expects 0 returning Future.unit
    userDataStorage.deleteTriggerNotification _ expects 1 returning Future.unit
    priceCache.getExchangeRate _ expects(eri.from, eri.to) returning Future.successful(eri) atLeastOnce()
    messageSender expects * twice()
    triggerNotificationHandler.checkTriggers().futureValue shouldBe (())
  }

  "TriggerNotificationHandler" should "send messages if PortfolioTriggerNotification triggers" in new TestWiring {
    val portfolio = Portfolio(0, 0, "p", USD, Map(si1.name -> 1))
    val notR = PortfolioTriggerNotification(0, 0, "p", 22, Raise)
    val notF = PortfolioTriggerNotification(1, 0, "p", 24, Fall)
    userDataStorage.getAllTriggerNotifications _ expects() returning Future.successful(Seq(notR, notF))
    userDataStorage.deleteTriggerNotification _ expects 0 returning Future.unit
    userDataStorage.deleteTriggerNotification _ expects 1 returning Future.unit
    userDataStorage.getPortfolio _ expects(0, "p") returning Future.successful(portfolio) atLeastOnce()
    priceCache.getStockInfo _ expects si1.name returning Future.successful(si1) atLeastOnce()
    messageSender expects * twice()
    triggerNotificationHandler.checkTriggers().futureValue shouldBe (())
  }

  "TriggerNotificationHandler" should "shouldn't crush if there is no portfolio in data base" in new TestWiring {
    val not = PortfolioTriggerNotification(0, 0, "p", 22, Raise)
    userDataStorage.getAllTriggerNotifications _ expects() returning Future.successful(Seq(not))
    userDataStorage.getPortfolio _ expects(0, "p") returning Future.failed(new NoSuchElementException)
    triggerNotificationHandler.checkTriggers().futureValue shouldBe (())
  }

  "TriggerNotificationHandler" should "send messages if stockTriggerNotification with BothNotification type triggers" in
    new TestWiring {
      val notB1 = StockTriggerNotification(0, 0, si1.name, 22, Both)
      val notB2 = StockTriggerNotification(1, 0, si1.name, 24, Both)
      userDataStorage.getAllTriggerNotifications _ expects() returning Future.successful(Seq(notB1, notB2)) atLeastOnce()
      userDataStorage.deleteTriggerNotification _ expects 0 returning Future.unit atLeastOnce()
      userDataStorage.deleteTriggerNotification _ expects 1 returning Future.unit atLeastOnce()
      inSequence {
        priceCache.getStockInfo _ expects si1.name returning Future.successful(si1) twice()
        priceCache.getStockInfo _ expects si1.name returning Future.successful(si1.copy(price = 25)) twice()
        priceCache.getStockInfo _ expects si1.name returning Future.successful(si1.copy(price = 20)) twice()
      }
      messageSender expects * atLeastTwice()
      triggerNotificationHandler.checkTriggers().futureValue shouldBe (())
      triggerNotificationHandler.checkTriggers().futureValue shouldBe (())
      triggerNotificationHandler.checkTriggers().futureValue shouldBe (())
    }

}

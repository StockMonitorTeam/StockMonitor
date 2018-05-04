package stockmonitoringbot.notificationhandlers

import info.mukel.telegrambot4s.methods.SendMessage
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}
import stockmonitoringbot.datastorage.models._
import stockmonitoringbot.{ActorSystemComponentImpl, ExecutionContextImpl}
import stockmonitoringbot.datastorage.{UserDataStorage, UserDataStorageComponent}
import stockmonitoringbot.messengerservices.MessageSenderComponent
import stockmonitoringbot.stockpriceservices._
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
    with ExecutionContextImpl {
    implicit val patienceConfig: PatienceConfig = PatienceConfig(500 millis, 20 millis)
    override val messageSender = mockFunction[SendMessage, Unit]
    override val priceCache = mock[PriceCache]
    override val userDataStorage = mock[UserDataStorage]
    override val stockPriceService = mock[StockPriceService]
  }

  val time = parseZonedDateTime("2018-04-20 15:05:00", "US/Eastern")
  val si1 = BaseStockInfo("MSFT", 23, Some(0), time)
  val si2 = BaseStockInfo("AAPL", 32, Some(0), time)
  val eri = CurrencyExchangeRateInfo("USD", "", "RUB", "", 23, time)

  "TriggerNotificationHandler" should "update cache" in new TestWiring {
    priceCache.getStocks _ expects() returning Set(si1.name, si2.name)
    priceCache.getExchangePairs _ expects() returning Set(eri.from -> eri.to)
    stockPriceService.getBatchPrices _ expects where { seq: Seq[String] =>
      seq.contains(si1.name) && seq.contains(si2.name)
    } returning Future.successful(Seq(si1, si2))
    stockPriceService.getCurrencyExchangeRate _ expects(eri.from, eri.to) returning Future.successful(eri)
    inAnyOrder {
      priceCache.setStockInfo _ expects si1
      priceCache.setStockInfo _ expects si2
    }
    priceCache.setExchangeRate _ expects eri
    triggerNotificationHandler.updateCache().futureValue shouldBe ((2, 1))
  }

  "TriggerNotificationHandler" should "send messages if stockTriggerNotification triggers" in new TestWiring {
    val notR = StockTriggerNotification(0, si1.name, 22, RaiseNotification)
    val notF = StockTriggerNotification(0, si1.name, 24, FallNotification)
    userDataStorage.getAllTriggerNotifications _ expects() returning Future.successful(Seq(notR, notF))
    userDataStorage.deleteTriggerNotification _ expects notR returning Future.unit
    userDataStorage.deleteTriggerNotification _ expects notF returning Future.unit
    priceCache.getStockInfo _ expects si1.name returning Future.successful(si1) atLeastOnce()
    messageSender expects * twice()
    triggerNotificationHandler.checkTriggers(priceCache, priceCache).futureValue shouldBe (())
  }

  "TriggerNotificationHandler" should "send messages if exchangeRateTriggerNotification triggers" in new TestWiring {
    val notR = ExchangeRateTriggerNotification(0, (eri.from, eri.to), 22, RaiseNotification)
    val notF = ExchangeRateTriggerNotification(0, (eri.from, eri.to), 24, FallNotification)
    userDataStorage.getAllTriggerNotifications _ expects() returning Future.successful(Seq(notR, notF))
    userDataStorage.deleteTriggerNotification _ expects notR returning Future.unit
    userDataStorage.deleteTriggerNotification _ expects notF returning Future.unit
    priceCache.getExchangeRate _ expects(eri.from, eri.to) returning Future.successful(eri) atLeastOnce()
    messageSender expects * twice()
    triggerNotificationHandler.checkTriggers(priceCache, priceCache).futureValue shouldBe (())
  }

  "TriggerNotificationHandler" should "send messages if PortfolioTriggerNotification triggers" in new TestWiring {
    val portfolio = Portfolio(0, "p", USD, Map(si1.name -> 1))
    val notR = PortfolioTriggerNotification(0, "p", 22, RaiseNotification)
    val notF = PortfolioTriggerNotification(0, "p", 24, FallNotification)
    userDataStorage.getAllTriggerNotifications _ expects() returning Future.successful(Seq(notR, notF))
    userDataStorage.deleteTriggerNotification _ expects notR returning Future.unit
    userDataStorage.deleteTriggerNotification _ expects notF returning Future.unit
    userDataStorage.getPortfolio _ expects(0, "p") returning Future.successful(portfolio) atLeastOnce()
    priceCache.getStockInfo _ expects si1.name returning Future.successful(si1) atLeastOnce()
    messageSender expects * twice()
    triggerNotificationHandler.checkTriggers(priceCache, priceCache).futureValue shouldBe (())
  }

  "TriggerNotificationHandler" should "shouldn't crush if there is no portfolio in data base" in new TestWiring {
    val not = PortfolioTriggerNotification(0, "p", 22, RaiseNotification)
    userDataStorage.getAllTriggerNotifications _ expects() returning Future.successful(Seq(not))
    userDataStorage.getPortfolio _ expects(0, "p") returning Future.failed(new NoSuchElementException)
    triggerNotificationHandler.checkTriggers(priceCache, priceCache).futureValue shouldBe (())
  }

  "TriggerNotificationHandler" should "send messages if stockTriggerNotification with BothNotification type triggers" in
    new TestWiring {
      val notB1 = StockTriggerNotification(0, si1.name, 22, BothNotification)
      val notB2 = StockTriggerNotification(0, si1.name, 24, BothNotification)
      userDataStorage.getAllTriggerNotifications _ expects() returning Future.successful(Seq(notB1, notB2)) twice()
      userDataStorage.deleteTriggerNotification _ expects notB1 returning Future.unit
      userDataStorage.deleteTriggerNotification _ expects notB2 returning Future.unit
      priceCache.getStockInfo _ expects si1.name returning Future.successful(si1) atLeastOnce()
      val newPriceCache1 = mock[PriceCache]
      newPriceCache1.getStockInfo _ expects si1.name returning Future.successful(si1.copy(price = 25)) atLeastOnce()
      val newPriceCache2 = mock[PriceCache]
      newPriceCache2.getStockInfo _ expects si1.name returning Future.successful(si1.copy(price = 20)) atLeastOnce()
      messageSender expects * twice()
      triggerNotificationHandler.checkTriggers(priceCache, newPriceCache1).futureValue shouldBe (())
      triggerNotificationHandler.checkTriggers(priceCache, newPriceCache2).futureValue shouldBe (())
    }

}

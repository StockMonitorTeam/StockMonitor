package stockmonitoringbot.notificationhandlers

import java.time.LocalTime

import info.mukel.telegrambot4s.methods.SendMessage
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}
import stockmonitoringbot.datastorage.models._
import stockmonitoringbot.datastorage.{UserDataStorage, UserDataStorageComponent}
import stockmonitoringbot.messengerservices.MessageSenderComponent
import stockmonitoringbot.stockpriceservices.models.{BaseStockInfo, CurrencyExchangeRateInfo}
import stockmonitoringbot.stockpriceservices.parseZonedDateTime
import stockmonitoringbot.stocksandratescache.{PriceCache, PriceCacheComponent}
import stockmonitoringbot.{ActorSystemComponentImpl, ExecutionContextImpl}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by amir.
  */
class DailyNotificationHandlerImplTest extends FlatSpec with Matchers with ScalaFutures with MockFactory {

  private trait TestWiring extends DailyNotificationHandlerComponentImpl
    with ActorSystemComponentImpl
    with ExecutionContextImpl
    with MessageSenderComponent
    with UserDataStorageComponent
    with PriceCacheComponent {
    implicit val patienceConfig: PatienceConfig = PatienceConfig(500 millis, 20 millis)
    override val messageSender = mockFunction[SendMessage, Unit]
    override val priceCache = mock[PriceCache]
    override val userDataStorage = mock[UserDataStorage]
  }

  val time = parseZonedDateTime("2018-04-20 15:05:00", "US/Eastern")

  "DailyNotificationHandler" should "notify users on StockDailyNotification" in new TestWiring {
    val stock = "MSFT"
    val notification = StockDailyNotification(0, stock, LocalTime.of(0, 0))
    priceCache.getStockInfo _ expects stock returning Future.successful(BaseStockInfo(stock, 23, Some(0), time))
    messageSender expects *
    dailyNotificationHandler.notifyUser(notification).futureValue shouldBe (())
  }

  "DailyNotificationHandler" should "notify users on ExchangeRateDailyNotification" in new TestWiring {
    val (from, to) = ("USD", "RUB")
    val notification = ExchangeRateDailyNotification(0, (from, to), LocalTime.of(0, 0))
    priceCache.getExchangeRate _ expects(from, to) returning Future.successful(CurrencyExchangeRateInfo(from, "", to, "", 23, time))
    messageSender expects *
    dailyNotificationHandler.notifyUser(notification).futureValue shouldBe (())
  }

  "DailyNotificationHandler" should "notify users on PortfolioDailyNotification" in new TestWiring {
    val portfolio = Portfolio(0, 0, "portfolio", USD, Map("MSFT" -> 1, "AAPL" -> 1))
    val notification = PortfolioDailyNotification(0, "portfolio", LocalTime.of(0, 0))
    userDataStorage.getPortfolio _ expects(0, "portfolio") returning Future.successful(portfolio)
    inAnyOrder {
      priceCache.getStockInfo _ expects "MSFT" returning Future.successful(BaseStockInfo("MSFT", 23, Some(0), time))
      priceCache.getStockInfo _ expects "AAPL" returning Future.successful(BaseStockInfo("AAPL", 32, Some(0), time))
    }
    messageSender expects *
    dailyNotificationHandler.notifyUser(notification).futureValue shouldBe (())
  }

}

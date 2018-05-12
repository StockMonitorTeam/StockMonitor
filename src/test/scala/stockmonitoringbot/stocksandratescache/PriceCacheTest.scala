package stockmonitoringbot.stocksandratescache

import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures
import stockmonitoringbot.{ActorSystemComponentImpl, ExecutionContextImpl}
import stockmonitoringbot.stockpriceservices._
import stockmonitoringbot.stockpriceservices.models.{BaseStockInfo, CurrencyExchangeRateInfo}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by amir.
  */
class PriceCacheTest extends FlatSpec with Matchers with ScalaFutures with MockFactory {

  trait TestWiring extends PriceCacheComponentImpl
    with StockPriceServiceComponent
    with ActorSystemComponentImpl
    with ExecutionContextImpl {
    implicit val patienceConfig: PatienceConfig = PatienceConfig(500 millis, 20 millis)
    override val stockPriceService = mock[StockPriceService]
    val stock1 = "MSFT"
    val stock2 = "YNDX"
    val stockInfo1 = BaseStockInfo(stock1, BigDecimal("94.8500"), Some(29033), parseZonedDateTime("2018-04-20 15:05:00", "US/Eastern"))
    val stockInfo2 = BaseStockInfo(stock2, BigDecimal("34.2900"), Some(3540531), parseZonedDateTime("2018-04-23 12:15:46", "US/Eastern"))
    val exchangePair1 = ("USD", "EUR")
    val exchangePair2 = ("USD", "RUB")
    val exchangeRateInfo1 = CurrencyExchangeRateInfo("USD", "United States Dollar",
      "EUR", "--",
      BigDecimal("1.81350000"), parseZonedDateTime("2018-04-23 15:41:12", "UTC"))
    val exchangeRateInfo2 = CurrencyExchangeRateInfo("USD", "United States Dollar",
      "RUB", "Russian Ruble",
      BigDecimal("61.81350000"), parseZonedDateTime("2018-04-23 15:41:12", "UTC"))

  }

  ///////TEST STOCKS

  "PriceCacheImpl" should "store StockInfo" in new TestWiring {
    priceCache.setStockInfo(stockInfo1)
    priceCache.contains(stock1) shouldBe true
    priceCache.contains(stock2) shouldBe false
    priceCache.getStockInfo(stock1).futureValue shouldBe stockInfo1
  }

  "PriceCacheImpl" should "return all stocks that it contain" in new TestWiring {
    priceCache.setStockInfo(stockInfo1)
    priceCache.setStockInfo(stockInfo2)
    priceCache.getStocks should contain theSameElementsAs Seq(stock1, stock2)
  }

  "PriceCacheImpl" should "should make request to stockPriceService, if it hasn't information about stock" in new TestWiring {
    stockPriceService.getStockPriceInfo _ expects stock1 returning Future.successful(stockInfo1)
    priceCache.getStockInfo(stock1).futureValue shouldBe stockInfo1
  }

  "PriceCacheImpl" should "should save stockPriceInfo after retrieving" in new TestWiring {
    stockPriceService.getStockPriceInfo _ expects stock1 returning Future.successful(stockInfo1)
    priceCache.getStockInfo(stock1).futureValue shouldBe stockInfo1
  }

  ////////TEST EXCHANGE RATES

  "PriceCacheImpl" should "store exchangeRateInfo" in new TestWiring {
    priceCache.setExchangeRate(exchangeRateInfo1)
    priceCache.contains(exchangePair1) shouldBe true
    priceCache.contains(exchangePair2) shouldBe false
    priceCache.getExchangeRate(exchangePair1._1, exchangePair1._2).futureValue shouldBe exchangeRateInfo1
  }

  "PriceCacheImpl" should "return all exchange rates that it contain" in new TestWiring {
    priceCache.setExchangeRate(exchangeRateInfo1)
    priceCache.setExchangeRate(exchangeRateInfo2)
    priceCache.getExchangePairs should contain theSameElementsAs Seq(exchangePair1, exchangePair2)
  }

  "PriceCacheImpl" should "should make request to stockPriceService, if it hasn't information about exchange rate" in new TestWiring {
    stockPriceService.getCurrencyExchangeRate _ expects(exchangePair1._1, exchangePair1._2) returning Future.successful(exchangeRateInfo1)
    priceCache.getExchangeRate(exchangePair1._1, exchangePair1._2).futureValue shouldBe exchangeRateInfo1
  }

  "PriceCacheImpl" should "should save exchangeRateInfo after retrieving" in new TestWiring {
    stockPriceService.getCurrencyExchangeRate _ expects(exchangePair1._1, exchangePair1._2) returning Future.successful(exchangeRateInfo1) once()
    priceCache.getExchangeRate(exchangePair1._1, exchangePair1._2).futureValue shouldBe exchangeRateInfo1
    priceCache.getExchangeRate(exchangePair1._1, exchangePair1._2).futureValue shouldBe exchangeRateInfo1
  }

  "PriceCacheImpl" should "should copy itself" in new TestWiring {
    priceCache.setExchangeRate(exchangeRateInfo1)
    priceCache.setStockInfo(stockInfo1)
    val copy = priceCache.copy()
    copy.getExchangeRate(exchangePair1._1, exchangePair1._2).futureValue shouldBe exchangeRateInfo1
    copy.getStockInfo(stock1).futureValue shouldBe stockInfo1
  }

}

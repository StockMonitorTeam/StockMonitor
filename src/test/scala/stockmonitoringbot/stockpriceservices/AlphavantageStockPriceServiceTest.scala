package stockmonitoringbot.stockpriceservices

import akka.http.scaladsl.model._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}
import stockmonitoringbot.stockpriceservices.exceptions.ServerResponseException
import stockmonitoringbot.{ActorSystemComponentImpl, ApiKeys, ExecutionContextImpl}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by amir.
  */

class AlphavantageStockPriceServiceTest extends FlatSpec with Matchers with ScalaFutures with MockFactory {

  val apiKey = "123"

  trait ApiKeysMock extends ApiKeys {
    override def getKey(keyPath: String): String = apiKey
  }

  private trait TestWiring {
    implicit val patienceConfig: PatienceConfig = PatienceConfig(500 millis, 20 millis)
    val stockPriceServiceMock = new AlphavantageStockPriceService
      with ActorSystemComponentImpl
      with ExecutionContextImpl
      with HttpRequestExecutor
      with ApiKeysMock {
      override val executeRequest =
        mockFunction[HttpRequest, Future[HttpResponse]]
    }
  }

  //////////////SINGLE REQUEST TESTS

  val stock = "MSFT"

  val responseEntity: String =
    """{
      |    "Meta Data": {
      |        "1. Information": "Intraday (1min) prices and volumes",
      |        "2. Symbol": "MSFT",
      |        "3. Last Refreshed": "2018-04-20 15:05:00",
      |        "4. Interval": "1min",
      |        "5. Output Size": "Compact",
      |        "6. Time Zone": "US/Eastern"
      |    },
      |    "Time Series (1min)": {
      |        "2018-04-20 15:05:00": {
      |            "1. open": "94.9200",
      |            "2. high": "94.9300",
      |            "3. low": "94.8350",
      |            "4. close": "94.8500",
      |            "5. volume": "29033"
      |        },
      |        "2018-04-20 15:04:00": {
      |            "1. open": "94.8625",
      |            "2. high": "94.9300",
      |            "3. low": "94.8600",
      |            "4. close": "94.9250",
      |            "5. volume": "27095"
      |        },
      |        "2018-04-20 15:03:00": {
      |            "1. open": "94.8700",
      |            "2. high": "94.8800",
      |            "3. low": "94.8600",
      |            "4. close": "94.8650",
      |            "5. volume": "16926"
      |        }
      |     }
      |  }""".stripMargin

  "AlphavantageStockPriceService" should "make correct request and correctly parse response in \"getStockPriceInfo\"" in new TestWiring {
    val expectedRequest = HttpRequest(uri = s"/query?function=TIME_SERIES_INTRADAY&symbol=$stock&interval=1min&apikey=$apiKey")
    stockPriceServiceMock.executeRequest
      .expects(expectedRequest)
      .returning(Future.successful(HttpResponse(
        entity = HttpEntity.apply(responseEntity).withContentType(ContentTypes.`application/json`))))
    val result: Future[StockInfo] = stockPriceServiceMock.getStockPriceInfo(stock)
    result.futureValue shouldBe
      DetailedStockInfo(stock, BigDecimal("94.9200"), BigDecimal("94.9300"), BigDecimal("94.8350"),
        BigDecimal("94.8500"), 29033, parseZonedDateTime("2018-04-20 15:05:00", "US/Eastern"))
  }

  "AlphavantageStockPriceService" should "return close price for the last segment" in new TestWiring {
    stockPriceServiceMock.executeRequest
      .expects(*)
      .returning(Future.successful(HttpResponse(
        entity = HttpEntity.apply(responseEntity).withContentType(ContentTypes.`application/json`))))
    val result: Future[StockInfo] = stockPriceServiceMock.getStockPriceInfo(stock)
    result.futureValue.price shouldBe BigDecimal("94.85")
  }

  //////////////BATCH REQUEST TESTS

  val batch = Seq("MSFT", "YNDX", "BAC")

  val batchResponse: String =
    """{
      |    "Meta Data": {
      |        "1. Information": "Batch Stock Market Quotes",
      |        "2. Notes": "IEX Real-Time Price provided for free by IEX (https://iextrading.com/developer/).",
      |        "3. Time Zone": "US/Eastern"
      |    },
      |    "Stock Quotes": [
      |        {
      |            "1. symbol": "MSFT",
      |            "2. price": "95.8200",
      |            "3. volume": "8412698",
      |            "4. timestamp": "2018-04-23 12:16:03"
      |        },
      |        {
      |            "1. symbol": "YNDX",
      |            "2. price": "34.2900",
      |            "3. volume": "3540531",
      |            "4. timestamp": "2018-04-23 12:15:46"
      |        },
      |        {
      |            "1. symbol": "BAC",
      |            "2. price": "30.3700",
      |            "3. volume": "21796573",
      |            "4. timestamp": "2018-04-23 12:16:03"
      |        }
      |    ]
      |}""".stripMargin

  "AlphavantageStockPriceService" should "make correct request and correctly parse response in \"getBatchPrices\"" in new TestWiring {
    val expectedRequest = HttpRequest(uri = s"/query?function=BATCH_STOCK_QUOTES&symbols=MSFT,YNDX,BAC&apikey=$apiKey")
    stockPriceServiceMock.executeRequest
      .expects(expectedRequest)
      .returning(Future.successful(
        HttpResponse(entity = HttpEntity.apply(batchResponse).withContentType(ContentTypes.`application/json`))))
    val result = stockPriceServiceMock.getBatchPrices(batch)
    result.futureValue should contain theSameElementsAs
      Seq(BaseStockInfo("MSFT", BigDecimal("95.8200"), 8412698, parseZonedDateTime("2018-04-23 12:16:03", "US/Eastern")),
        BaseStockInfo("YNDX", BigDecimal("34.2900"), 3540531, parseZonedDateTime("2018-04-23 12:15:46", "US/Eastern")),
        BaseStockInfo("BAC", BigDecimal("30.3700"), 21796573, parseZonedDateTime("2018-04-23 12:16:03", "US/Eastern"))
      )
  }

  "AlphavantageStockPriceService" should "shouldn't make any requests if batch is empty in \"getBatchPrices\"" in new TestWiring {
    val result = stockPriceServiceMock.getBatchPrices(Seq.empty)
    result.futureValue shouldBe Seq.empty
  }

  "AlphavantageStockPriceService" should "make 2 requests if there are 101 stock in batch" in new TestWiring {
    val batch101 = (1 to 101).map(_ => "A")
    val expectedRequest1 = HttpRequest(uri = s"/query?function=BATCH_STOCK_QUOTES&symbols=${(1 to 100).map(_ => "A").mkString(",")}&apikey=$apiKey")
    val expectedRequest2 = HttpRequest(uri = s"/query?function=BATCH_STOCK_QUOTES&symbols=A&apikey=$apiKey")
    inAnyOrder {
      stockPriceServiceMock.executeRequest
        .expects(expectedRequest1)
        .returning(Future.successful(HttpResponse(status = StatusCodes.Forbidden)))
      stockPriceServiceMock.executeRequest
        .expects(expectedRequest2)
        .returning(Future.successful(HttpResponse(status = StatusCodes.Forbidden)))
    }
    stockPriceServiceMock.getBatchPrices(batch101).failed.futureValue shouldBe a[ServerResponseException]
  }

  //////////////CURRENCY EXCHANGE TESTS

  val from = "USD"
  val to = "RUB"

  val currencyExchangeResponseEntity: String =
    """{
      |    "Realtime Currency Exchange Rate": {
      |        "1. From_Currency Code": "USD",
      |        "2. From_Currency Name": "United States Dollar",
      |        "3. To_Currency Code": "RUB",
      |        "4. To_Currency Name": "Russian Ruble",
      |        "5. Exchange Rate": "61.81350000",
      |        "6. Last Refreshed": "2018-04-23 15:41:12",
      |        "7. Time Zone": "UTC"
      |    }
      |}""".stripMargin

  "AlphavantageStockPriceService" should "make correct request and correctly parse response in \"getCurrencyExchangeRate\"" in new TestWiring {
    val expectedRequest = HttpRequest(uri = s"/query?function=CURRENCY_EXCHANGE_RATE&from_currency=$from&to_currency=$to&apikey=$apiKey")
    stockPriceServiceMock.executeRequest
      .expects(expectedRequest)
      .returning(Future.successful(HttpResponse(
        entity = HttpEntity.apply(currencyExchangeResponseEntity).withContentType(ContentTypes.`application/json`))))
    val result = stockPriceServiceMock.getCurrencyExchangeRate(from, to)
    result.futureValue shouldBe
      CurrencyExchangeRateInfo("USD", "United States Dollar",
        "RUB", "Russian Ruble",
        BigDecimal("61.81350000"), parseZonedDateTime("2018-04-23 15:41:12", "UTC"))
  }
}
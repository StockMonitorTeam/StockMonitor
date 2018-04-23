package stockmonitoringbot.stockpriceservices

import akka.http.scaladsl.model._
import org.scalatest.{FlatSpec, Matchers}
import stockmonitoringbot.{ActorSystemComponentImpl, ApiKeys, ExecutionContextImpl}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}
import scala.util.Success

/**
  * Created by amir.
  */

class AlphavantageStockPriceServiceTest extends FlatSpec with Matchers {
  var currentRequest: HttpRequest = _
  var currentPromise: Promise[HttpResponse] = _

  val apiKey = "123"

  trait HttpRequestExecutorMock extends HttpRequestExecutor {
    override def executeRequest(request: HttpRequest): Future[HttpResponse] = {
      currentRequest = request
      currentPromise = Promise()
      currentPromise.future
    }
  }

  trait ApiKeysMock extends ApiKeys {
    override def getKey(keyPath: String): String = apiKey
  }

  val stockPriceService = new {}
    with AlphavantageStockPriceService
    with ActorSystemComponentImpl
    with ExecutionContextImpl
    with HttpRequestExecutorMock
    with ApiKeysMock

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

  "AlphavantageStockPriceService" should "create correct single stock info HttpRequest" in {
    stockPriceService.getStockPriceInfo(stock)
    currentRequest shouldBe
      HttpRequest(uri = s"/query?function=TIME_SERIES_INTRADAY&symbol=$stock&interval=1min&apikey=$apiKey")
  }

  "AlphavantageStockPriceService" should "parse correctly single stock info json" in {
    val result: Future[StockInfo] = stockPriceService.getStockPriceInfo(stock)
    currentPromise.complete(Success(HttpResponse(
      entity = HttpEntity.apply(responseEntity).withContentType(ContentTypes.`application/json`))))
    Await.result(result, Duration.Inf) shouldBe
      DetailedStockInfo(stock, 94.9200, 94.9300, 94.8350, 94.8500, 29033,
        parseZonedDateTime("2018-04-20 15:05:00", "US/Eastern"))
  }

  "AlphavantageStockPriceService" should "fail if service is unavailable" in {
    val result = stockPriceService.getStockPriceInfo(stock)
    currentPromise.complete(Success(HttpResponse(status = StatusCodes.BadRequest)))
    an[Exception] should be thrownBy Await.result(result, Duration.Inf)
  }

  //////////////BATCH REQUEST TESTS

  val batch = Seq("MSFT", "YNDX", "BAC")

  val batchResponse =
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

  "AlphavantageStockPriceService" should "create correct batch info HttpRequest" in {
    stockPriceService.getBatchPrices(batch)
    currentRequest shouldBe
      HttpRequest(uri = s"/query?function=BATCH_STOCK_QUOTES&symbols=MSFT,YNDX,BAC&apikey=$apiKey")
  }

  "AlphavantageStockPriceService" should "parse correctly batch info json" in {
    val result = stockPriceService.getBatchPrices(batch)
    currentPromise.complete(Success(HttpResponse(
      entity = HttpEntity.apply(batchResponse).withContentType(ContentTypes.`application/json`))))
    Await.result(result, Duration.Inf) should contain theSameElementsAs
      Seq(BaseStockInfo("MSFT", 95.8200, 8412698, parseZonedDateTime("2018-04-23 12:16:03", "US/Eastern")),
        BaseStockInfo("YNDX", 34.2900, 3540531, parseZonedDateTime("2018-04-23 12:15:46", "US/Eastern")),
        BaseStockInfo("BAC", 30.3700, 21796573, parseZonedDateTime("2018-04-23 12:16:03", "US/Eastern"))
      )
  }

  //////////////CURRENCY EXCHANGE TESTS

  val from = "USD"
  val to = "RUB"

  "AlphavantageStockPriceService" should "create correct exchange info HttpRequest" in {
    stockPriceService.getCurrencyExchangeRate(from, to)
    currentRequest shouldBe HttpRequest(
      uri = s"/query?function=CURRENCY_EXCHANGE_RATE&from_currency=$from&to_currency=$to&apikey=$apiKey")
  }

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

  "AlphavantageStockPriceService" should "parse correctly exchange info json" in {
    val result = stockPriceService.getCurrencyExchangeRate(from, to)
    currentPromise.complete(Success(HttpResponse(
      entity = HttpEntity.apply(currencyExchangeResponseEntity).
        withContentType(ContentTypes.`application/json`))))
    Await.result(result, Duration.Inf) shouldBe
      CurrencyExchangeRateInfo("USD", "United States Dollar",
        "RUB", "Russian Ruble",
        61.81350000, parseZonedDateTime("2018-04-23 15:41:12", "UTC"))
  }


}



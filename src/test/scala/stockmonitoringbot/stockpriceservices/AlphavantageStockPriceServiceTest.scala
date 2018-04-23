package stockmonitoringbot.stockpriceservices

import akka.http.scaladsl.model._
import akka.stream.scaladsl.Flow
import org.scalatest.{FlatSpec, Matchers}
import stockmonitoringbot.{ActorSystemComponentImpl, ApiKeys, ExecutionContextImpl}

import scala.collection.concurrent.{Map, TrieMap}
import scala.concurrent.duration._
import scala.concurrent.{Await, Future, Promise}
import scala.language.postfixOps
import scala.util.{Success, Try}

/**
  * Created by amir.
  */

class AlphavantageStockPriceServiceTest extends FlatSpec with Matchers {
  var currentRequests: Map[HttpRequest, Promise[HttpResponse]] = TrieMap.empty

  val apiKey = "123"

  trait ApiKeysMock extends ApiKeys {
    override def getKey(keyPath: String): String = apiKey
  }

  private val stockPriceService = new {}
    with AlphavantageStockPriceService
    with ActorSystemComponentImpl
    with ExecutionContextImpl
    with ApiKeysMock {
    override lazy val queriesPerSecond = 100
    override lazy val pool: Flow[(HttpRequest, Promise[HttpResponse]), (Try[HttpResponse], Promise[HttpResponse]), Any] =
      Flow.apply[(HttpRequest, Promise[HttpResponse])].mapAsyncUnordered(4) { in =>
        val currentPromise = Promise[HttpResponse]()
        currentRequests.put(in._1, currentPromise)
        currentPromise.future.map(response => (Try(response), in._2))
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


  "AlphavantageStockPriceService" should "make correct request and correctly parse response in \"getStockPriceInfo\"" in {
    val result: Future[StockInfo] = stockPriceService.getStockPriceInfo(stock)
    val expectedRequest = HttpRequest(uri = s"/query?function=TIME_SERIES_INTRADAY&symbol=$stock&interval=1min&apikey=$apiKey")
    Thread.sleep(100)
    currentRequests.get(expectedRequest) should not be None
    currentRequests(expectedRequest).complete(Success(HttpResponse(
      entity = HttpEntity.apply(responseEntity).withContentType(ContentTypes.`application/json`))))
    Await.result(result, 1 second) shouldBe
      DetailedStockInfo(stock, 94.9200, 94.9300, 94.8350, 94.8500, 29033,
        parseZonedDateTime("2018-04-20 15:05:00", "US/Eastern"))
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

  "AlphavantageStockPriceService" should "make correct request and correctly parse response in \"getBatchPrices\"" in {
    val result = stockPriceService.getBatchPrices(batch)
    val expectedRequest = HttpRequest(uri = s"/query?function=BATCH_STOCK_QUOTES&symbols=MSFT,YNDX,BAC&apikey=$apiKey")
    Thread.sleep(100)
    currentRequests.get(expectedRequest) should not be None
    currentRequests(expectedRequest).complete(Success(HttpResponse(
      entity = HttpEntity.apply(batchResponse).withContentType(ContentTypes.`application/json`))))
    Await.result(result, 1 second) should contain theSameElementsAs
      Seq(BaseStockInfo("MSFT", 95.8200, 8412698, parseZonedDateTime("2018-04-23 12:16:03", "US/Eastern")),
        BaseStockInfo("YNDX", 34.2900, 3540531, parseZonedDateTime("2018-04-23 12:15:46", "US/Eastern")),
        BaseStockInfo("BAC", 30.3700, 21796573, parseZonedDateTime("2018-04-23 12:16:03", "US/Eastern"))
      )
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

  "AlphavantageStockPriceService" should "make correct request and correctly parse response in \"getCurrencyExchangeRate\"" in {
    val result = stockPriceService.getCurrencyExchangeRate(from, to)
    val expectedRequest = HttpRequest(uri = s"/query?function=CURRENCY_EXCHANGE_RATE&from_currency=$from&to_currency=$to&apikey=$apiKey")
    Thread.sleep(100)
    currentRequests.get(expectedRequest) should not be None
    currentRequests(expectedRequest).complete(Success(HttpResponse(
      entity = HttpEntity.apply(currencyExchangeResponseEntity).
        withContentType(ContentTypes.`application/json`))))
    Await.result(result, 1 second) shouldBe
      CurrencyExchangeRateInfo("USD", "United States Dollar",
        "RUB", "Russian Ruble",
        61.81350000, parseZonedDateTime("2018-04-23 15:41:12", "UTC"))
  }
}
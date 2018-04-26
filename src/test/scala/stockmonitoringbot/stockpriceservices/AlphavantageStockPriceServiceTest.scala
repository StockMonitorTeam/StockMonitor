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

  val stock = "MSFT"
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


  "AlphavantageStockPriceService" should "create correct HttpRequest" in {
    stockPriceService.getStockPriceInfo(stock)
    currentRequest shouldBe
      HttpRequest(uri = s"/query?function=TIME_SERIES_INTRADAY&symbol=$stock&interval=1min&apikey=$apiKey")
  }

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

  "AlphavantageStockPriceService" should "parse correctly HttpResponse" in {
    val result: Future[StockInfo] = stockPriceService.getStockPriceInfo(stock)
    currentPromise.complete(Success(HttpResponse(
      entity = HttpEntity.apply(responseEntity).withContentType(ContentTypes.`application/json`))))
    Await.result(result, Duration.Inf) shouldBe
      StockInfo(stock, 94.9200, 94.9300, 94.8350, 94.8500, 29033, "2018-04-20 15:05:00")
  }

  "AlphavantageStockPriceService" should "fail if service is unavailable" in {
    val result = stockPriceService.getStockPriceInfo(stock)
    currentPromise.complete(Success(HttpResponse(status = StatusCodes.BadRequest)))
    an[Exception] should be thrownBy Await.result(result, Duration.Inf)
  }

  "AlphavantageStockPriceService" should "return close price for the last segment" in {
    val result: Future[StockInfo] = stockPriceService.getStockPriceInfo(stock)
    currentPromise.complete(Success(HttpResponse(
      entity = HttpEntity.apply(responseEntity).withContentType(ContentTypes.`application/json`))))
    Await.result(result, Duration.Inf).price shouldBe 94.85
  }

}

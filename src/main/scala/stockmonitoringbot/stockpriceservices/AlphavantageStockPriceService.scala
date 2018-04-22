package stockmonitoringbot.stockpriceservices

import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import spray.json.JsValue
import stockmonitoringbot.{ActorSystemComponent, ApiKeys, ExecutionContextComponent}

import scala.concurrent.Future
import scala.util.{Failure, Try}

/**
  * Created by amir.
  */
trait AlphavantageStockPriceService extends StockPriceService {
  self: ActorSystemComponent
    with ExecutionContextComponent
    with HttpRequestExecutor
    with ApiKeys =>

  private val apiKey: String = getKey("StockMonitor.Alphavantage.apikey")

  //https://www.alphavantage.co/query?function=TIME_SERIES_INTRADAY&symbol=MSFT&interval=1min&apikey=demo
  private val stockPriceEndPoint = "/query"

  private def stockPriceRequest(stockName: String): HttpRequest = {
    val params = Map("function" -> "TIME_SERIES_INTRADAY",
      "symbol" -> stockName,
      "interval" -> "1min",
      "apikey" -> apiKey)
    HttpRequest(method = HttpMethods.GET,
      uri = Uri(stockPriceEndPoint).withQuery(Query(params)))
  }

  private def parseResult(json: JsValue): Try[StockInfo] = {
    Try {
      import spray.json.DefaultJsonProtocol._
      val stockName = json.asJsObject.fields("Meta Data").asJsObject.fields("2. Symbol").convertTo[String]
      val prices: Map[String, JsValue] = json.asJsObject.fields("Time Series (1min)").asJsObject.fields
      val latestTime = prices.keys.max
      val latestPrice = prices(latestTime).asJsObject.fields
      StockInfo(stockName,
        latestPrice("1. open").convertTo[String].toDouble,
        latestPrice("2. high").convertTo[String].toDouble,
        latestPrice("3. low").convertTo[String].toDouble,
        latestPrice("4. close").convertTo[String].toDouble,
        latestPrice("5. volume").convertTo[String].toInt,
        latestTime)
    }.recoverWith {
      case _ => Failure(new Exception(s"Can't parse $json"))
    }
  }

  override def getStockPriceInfo(stockName: String): Future[StockInfo] = {
    import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.sprayJsValueUnmarshaller
    executeRequest(stockPriceRequest(stockName)).flatMap({
      case HttpResponse(StatusCodes.OK, _, entity, _) =>
        Unmarshal(entity).to[JsValue].flatMap(result => Future.fromTry(parseResult(result)))
      case HttpResponse(code, _, entity, _) =>
        Unmarshal(entity).to[String].flatMap(message =>
          Future.failed(new Exception(s"bad request: $code, message: $message"))
        )
    })
  }
}

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

  private val endPoint = "/query"

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.sprayJsValueUnmarshaller
  import spray.json.DefaultJsonProtocol._

  private def singleStockParseResult(json: JsValue): Try[DetailedStockInfo] = {
    Try {
      val stockName = json.asJsObject.fields("Meta Data").asJsObject.fields("2. Symbol").convertTo[String]
      val zone = json.asJsObject.fields("Meta Data").asJsObject.fields("6. Time Zone").convertTo[String]
      val prices: Map[String, JsValue] = json.asJsObject.fields("Time Series (1min)").asJsObject.fields
      val latestTime = prices.keys.max
      val latestPrice = prices(latestTime).asJsObject.fields
      DetailedStockInfo(stockName,
        latestPrice("1. open").convertTo[String].toDouble,
        latestPrice("2. high").convertTo[String].toDouble,
        latestPrice("3. low").convertTo[String].toDouble,
        latestPrice("4. close").convertTo[String].toDouble,
        latestPrice("5. volume").convertTo[String].toInt,
        parseZonedDateTime(latestTime, zone))
    }.recoverWith {
      case _ => Failure(new Exception(s"Can't parse $json"))
    }
  }

  private def batchParseResult(json: JsValue): Try[Seq[BaseStockInfo]] = {
    Try {
      val zone = json.asJsObject.fields("Meta Data").asJsObject.fields("3. Time Zone").convertTo[String]
      val stocks: Seq[JsValue] = json.asJsObject.fields("Stock Quotes").convertTo[List[JsValue]]
      stocks.map(_.asJsObject.fields).map(json =>
        BaseStockInfo(
          json("1. symbol").convertTo[String],
          json("2. price").convertTo[String].toDouble,
          json("3. volume").convertTo[String].toInt,
          parseZonedDateTime(json("4. timestamp").convertTo[String], zone))
      )
    }.recoverWith {
      case _ => Failure(new Exception(s"Can't parse $json"))
    }
  }

  private def currencyExchangeRateParseResult(json: JsValue): Try[CurrencyExchangeRateInfo] = {
    Try {
      val info: Map[String, JsValue] =
        json.asJsObject.fields("Realtime Currency Exchange Rate").asJsObject.fields
      CurrencyExchangeRateInfo(
        info("1. From_Currency Code").convertTo[String],
        info("2. From_Currency Name").convertTo[String],
        info("3. To_Currency Code").convertTo[String],
        info("4. To_Currency Name").convertTo[String],
        info("5. Exchange Rate").convertTo[String].toDouble,
        parseZonedDateTime(info("6. Last Refreshed").convertTo[String],
          info("7. Time Zone").convertTo[String]))
    }.recoverWith {
      case _ => Failure(new Exception(s"Can't parse $json"))
    }
  }

  private def execAndParse[T](httpRequest: HttpRequest, jsonParser: JsValue => Try[T]): Future[T] = {
    executeRequest(httpRequest).flatMap({
      case HttpResponse(StatusCodes.OK, _, entity, _) =>
        Unmarshal(entity).to[JsValue].flatMap(result => Future.fromTry(jsonParser(result)))
      case HttpResponse(code, _, entity, _) =>
        Unmarshal(entity).to[String].flatMap(message =>
          Future.failed(new Exception(s"bad request: $code, message: $message"))
        )
    })
  }

  override def getStockPriceInfo(stockName: String): Future[DetailedStockInfo] = {
    val params = Map("function" -> "TIME_SERIES_INTRADAY",
      "symbol" -> stockName,
      "interval" -> "1min",
      "apikey" -> apiKey)
    val request = HttpRequest(uri = Uri(endPoint).withQuery(Query(params)))
    execAndParse(request, singleStockParseResult)
  }

  override def getBatchPrices(stocks: Seq[String]): Future[Seq[BaseStockInfo]] = {
    val params = Map("function" -> "BATCH_STOCK_QUOTES",
      "symbols" -> stocks.mkString(","),
      "apikey" -> apiKey)
    val request = HttpRequest(uri = Uri(endPoint).withQuery(Query(params)))
    execAndParse(request, batchParseResult)
  }

  override def getCurrencyExchangeRate(from: String, to: String): Future[CurrencyExchangeRateInfo] = {
    val params = Map("function" -> "CURRENCY_EXCHANGE_RATE",
      "from_currency" -> from,
      "to_currency" -> to,
      "apikey" -> apiKey)
    val request = HttpRequest(uri = Uri(endPoint).withQuery(Query(params)))
    execAndParse(request, currencyExchangeRateParseResult)
  }
}

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy, QueueOfferResult}
import spray.json.JsValue

import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success, Try}

/**
  * Created by amir.
  */
class AlphavantageStockPriceService(apiKey: String)(implicit actorSystem: ActorSystem) extends StockPriceService {

  implicit val materializer: ActorMaterializer = ActorMaterializer()

  import actorSystem.dispatcher

  //https://www.alphavantage.co/query?function=TIME_SERIES_INTRADAY&symbol=MSFT&interval=1min&apikey=demo
  private val ApiURL = "www.alphavantage.co"
  private val stockPriceEndPoint = "/query"

  private val pool = Http().cachedHostConnectionPoolHttps[Promise[HttpResponse]](host = ApiURL)
  private val queue = Source.queue[(HttpRequest, Promise[HttpResponse])](1000, OverflowStrategy.dropHead)
    .via(pool)
    .toMat(Sink.foreach {
      case ((Success(resp), p)) => p.success(resp)
      case ((Failure(e), p)) => p.failure(e)
    })(Keep.left)
    .run

  private def queueRequest(request: HttpRequest): Future[HttpResponse] = {
    val responsePromise = Promise[HttpResponse]()
    queue.offer(request -> responsePromise).flatMap {
      case QueueOfferResult.Enqueued => responsePromise.future
      case QueueOfferResult.Dropped => Future.failed(new RuntimeException("Queue overflowed. Try again later."))
      case QueueOfferResult.Failure(ex) => Future.failed(ex)
      case QueueOfferResult.QueueClosed => Future.failed(new RuntimeException("Queue was closed (pool shut down) while running the request. Try again later."))
    }
  }

  private def stockPriceRequest(stockName: String): HttpRequest = {
    val params = Map("function" -> "TIME_SERIES_INTRADAY",
      "symbol" -> stockName,
      "interval" -> "1min",
      "apikey" -> apiKey)
    HttpRequest(method = HttpMethods.GET,
      uri = Uri(stockPriceEndPoint).withQuery(Query(params)))
  }

  private def parseResult(json: JsValue): StockInfo = {
    Try[StockInfo] {
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
    }.getOrElse(throw new Exception(s"Can't parse json: $json"))
  }

  override def getStockPriceInfo(stockName: String): Future[StockInfo] = {
    import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.sprayJsValueUnmarshaller
    queueRequest(stockPriceRequest(stockName)).flatMap({
      case HttpResponse(StatusCodes.OK, _, entity, _) =>
        Unmarshal(entity).to[JsValue].map(parseResult)
      case HttpResponse(code, _, entity, _) =>
        Future.failed(new Exception(s"bad request: $code, message: $entity"))
    })
  }
}

package stockmonitoringbot.stockpriceservices

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{OverflowStrategy, QueueOfferResult, ThrottleMode}
import stockmonitoringbot.{ActorSystemComponent, ExecutionContextComponent}

import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}
import scala.language.postfixOps
import scala.util.{Failure, Success}

/**
  * Created by amir.
  */
trait HttpRequestExecutor {
  def executeRequest(request: HttpRequest): Future[HttpResponse]
}

trait AlphavantageHttpRequestExecutor extends HttpRequestExecutor {
  self: ActorSystemComponent with ExecutionContextComponent =>

  private val ApiURL = "www.alphavantage.co"

  private val pool = Http().cachedHostConnectionPoolHttps[Promise[HttpResponse]](ApiURL)
  private val queue = Source.queue[(HttpRequest, Promise[HttpResponse])](1000, OverflowStrategy.dropHead)
    .throttle(1, 1 second, 1, ThrottleMode.Shaping)
    .via(pool)
    .toMat(Sink.foreach {
      case ((Success(resp), p)) => p.success(resp)
      case ((Failure(e), p)) => p.failure(e)
    })(Keep.left)
    .run

  override def executeRequest(request: HttpRequest): Future[HttpResponse] = {
    val responsePromise = Promise[HttpResponse]()
    queue.offer(request -> responsePromise).flatMap {
      case QueueOfferResult.Enqueued => responsePromise.future
      case QueueOfferResult.Dropped => Future.failed(new RuntimeException("Queue overflowed. Try again later."))
      case QueueOfferResult.Failure(ex) => Future.failed(ex)
      case QueueOfferResult.QueueClosed => Future.failed(new RuntimeException("Queue was closed (pool shut down) while running the request. Try again later."))
    }
  }
}
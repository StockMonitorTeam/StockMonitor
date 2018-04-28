package stockmonitoringbot.stockpriceservices

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{OverflowStrategy, QueueOfferResult, ThrottleMode}
import stockmonitoringbot.{ActorSystemComponent, ExecutionContextComponent}

import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

/**
  * Created by amir.
  */
trait HttpRequestExecutor {
  val executeRequest: HttpRequest => Future[HttpResponse]
}

trait AlphavantageHttpRequestExecutor extends HttpRequestExecutor {
  self: ActorSystemComponent
    with ExecutionContextComponent =>

  private val ApiURL = "www.alphavantage.co"
  lazy val queriesPerSecond = 1
  lazy val pool: Flow[(HttpRequest, Promise[HttpResponse]), (Try[HttpResponse], Promise[HttpResponse]), Any] =
    Http().cachedHostConnectionPoolHttps[Promise[HttpResponse]](ApiURL)
  private val queue = Source.queue[(HttpRequest, Promise[HttpResponse])](1000, OverflowStrategy.dropNew)
    .throttle(queriesPerSecond, 1 second, 1, ThrottleMode.Shaping)
    .via(pool)
    .toMat(Sink.foreach {
      case ((Success(resp), p)) => p.success(resp)
      case ((Failure(e), p)) => p.failure(e)
    })(Keep.left)
    .run

  override val executeRequest: HttpRequest => Future[HttpResponse] = (request: HttpRequest) => {
    val responsePromise = Promise[HttpResponse]()
    queue.offer(request -> responsePromise).flatMap {
      case QueueOfferResult.Enqueued => responsePromise.future
      case QueueOfferResult.Dropped => Future.failed(new RuntimeException("Queue overflowed. Try again later."))
      case QueueOfferResult.Failure(ex) => Future.failed(ex)
      case QueueOfferResult.QueueClosed => Future.failed(new RuntimeException("Queue was closed (pool shut down) while running the request. Try again later."))
    }
  }

}

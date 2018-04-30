package stockmonitoringbot.notificationhandlers

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}
import stockmonitoringbot.datastorage.models.{Portfolio, USD}
import stockmonitoringbot.stockpriceservices.BaseStockInfo
import stockmonitoringbot.stocksandratescache.{PriceCache, PriceCacheComponent}
import stockmonitoringbot.{ActorSystemComponentImpl, ExecutionContextImpl}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by amir.
  */
class PortfolioPriceCalculationTest extends FlatSpec with Matchers with ScalaFutures with MockFactory {

  private trait TestWiring extends ActorSystemComponentImpl
    with ExecutionContextImpl
    with PriceCacheComponent {
    implicit val patienceConfig: PatienceConfig = PatienceConfig(500 millis, 20 millis)
    override val priceCache = mock[PriceCache]
  }

  val portfolio = Portfolio(0, "portfolio", USD, Map("MSFT" -> 3, "AAPL" -> 5))

  "getPortfolioCurrentPrice" should "calculate portfolio price" in new TestWiring {
    inAnyOrder {
      priceCache.getStockInfo _ expects "MSFT" returning Future.successful(BaseStockInfo("MSFT", 23, 0, null))
      priceCache.getStockInfo _ expects "AAPL" returning Future.successful(BaseStockInfo("AAPL", 32, 0, null))
    }
    getPortfolioCurrentPrice(portfolio, priceCache).futureValue shouldBe 3 * 23 + 5 * 32
  }
}

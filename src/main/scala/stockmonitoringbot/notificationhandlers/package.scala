package stockmonitoringbot

import stockmonitoringbot.datastorage.models.Portfolio
import stockmonitoringbot.stocksandratescache.PriceCache

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by amir.
  */
package object notificationhandlers {
  def getPortfolioCurrentPrice(portfolio: Portfolio, cache: PriceCache)
                              (implicit executionContext: ExecutionContext): Future[BigDecimal] = {
    Future.traverse(portfolio.stocks.keys)(cache.getStockInfo).map {
      _.foldRight(BigDecimal(0))((stock, sum) => sum + stock.price * portfolio.stocks(stock.name))
    }
  }
  def getPortfolioStocksPrice(portfolio: Portfolio, cache: PriceCache)
                             (implicit executionContext: ExecutionContext): Future[Map[String,BigDecimal]] = {
    Future.traverse(portfolio.stocks.keys)(cache.getStockInfo).map {
      _.map( stock => (stock.name, stock.price * portfolio.stocks(stock.name))).toMap
    }
  }

  val q = "\""
}

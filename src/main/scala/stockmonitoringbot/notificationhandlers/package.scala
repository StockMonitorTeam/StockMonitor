package stockmonitoringbot

import stockmonitoringbot.datastorage.models.Portfolio
import stockmonitoringbot.stocksandratescache.StocksAndExchangeRatesCache

import scala.concurrent.Future

/**
  * Created by amir.
  */
package object notificationhandlers {
  def getPortfolioCurrentPrice(portfolio: Portfolio, cache: StocksAndExchangeRatesCache): Future[BigDecimal] = {
    Future.traverse(portfolio.stocks.keys)(cache.getStockInfo).map {
      _.foldRight(BigDecimal(0))((stock, sum) => sum + stock.price * portfolio.stocks(stock.name))
    }
  }
}

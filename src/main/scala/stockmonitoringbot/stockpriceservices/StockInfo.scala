package stockmonitoringbot.stockpriceservices

/**
  * Created by amir.
  */
case class StockInfo(name: String, open: Double, high: Double, low: Double, close: Double, volume: Int, timeStamp: String) {
  val price: Double = (high + low) / 2 //todo correct formula for stock price
}

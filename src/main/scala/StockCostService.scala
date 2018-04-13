import scala.concurrent.Future

/**
  * Created by amir.
  */
trait StockCostService {
  def getCost(stockName: String): Future[StockInfo]
}

package stockmonitoringbot.datastorage.models

/**
  * Created by amir.
  */
trait AssetType
case class PortfolioAsset(name: String) extends AssetType
case class StockAsset(name: String) extends AssetType
case class ExchangeRateAsset(from: String, to: String) extends AssetType

package stockmonitoringbot.datastorage.models

case class Portfolio(portfolioId: Long,
                     userId: Long,
                     name: String,
                     currency: Currency,
                     stocks: Map[String, Double])

sealed trait Currency
case object USD extends Currency
case object EUR extends Currency
case object RUB extends Currency

object Currency {
  def define(currency: String): Currency = currency match {
    case "EUR" => EUR
    case "RUB" => RUB
    case "USD" => USD
    case _ => throw new IllegalStateException()
  }
}
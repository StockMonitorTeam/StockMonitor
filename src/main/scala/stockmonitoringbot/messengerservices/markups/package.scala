package stockmonitoringbot.messengerservices

import stockmonitoringbot.datastorage.models._

/**
  * Created by amir.
  */
package object markups {
  def tnToStr(trigger: TriggerNotification): String = {
    trigger match {
      case StockTriggerNotification(_, name, bound, nType) =>
        s"Акции $name $nType - $bound"
      case ExchangeRateTriggerNotification(_, (from, to), bound, nType) =>
        s"Курс $from/$to $nType - $bound"
      case PortfolioTriggerNotification(_, name, bound, nType) =>
        s"Портфель $name $nType - $bound"
    }
  }
  def dnToStr(notification: DailyNotification): String = {
    notification match {
      case StockDailyNotification(_, name, time) =>
        s"Акции $name в $time"
      case ExchangeRateDailyNotification(_, (from, to), time) =>
        s"Курс $from/$to в $time"
      case PortfolioDailyNotification(_, name, time) =>
        s"Портфель $name в $time"
    }
  }
}

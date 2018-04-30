package stockmonitoringbot.datastorage.models

import java.time.LocalTime

/**
  * Created by amir.
  */
trait Notification {
  val ownerId: Long
}

trait DailyNotification extends Notification {
  val time: LocalTime
}

trait TriggerNotification extends Notification {
  val notificationType: TriggerNotificationType
  val boundPrice: BigDecimal
}

sealed trait TriggerNotificationType
case object RaiseNotification extends TriggerNotificationType
case object FallNotification extends TriggerNotificationType

///

trait PortfolioNotification extends Notification {
  val portfolioName: String
}

trait ExchangeRateNotification extends Notification {
  val exchangePair: (String, String)
}

trait StockNotification extends Notification {
  val stock: String
}
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
case object Raise extends TriggerNotificationType {
  override def toString: String = "📈"
}
case object Fall extends TriggerNotificationType {
  override def toString: String = "📉"
}
case object Both extends TriggerNotificationType {
  override def toString: String = "📈➕📉"
}

object TriggerNotificationType {
  // TODO: Move symbols to interface
  def define(notificationType: String): TriggerNotificationType = notificationType match {
    case "📉" => Fall
    case "📈" => Raise
    case "📈➕📉" => Both
    case _ => throw new IllegalStateException()
  }
}

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
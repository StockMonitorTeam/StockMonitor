package stockmonitoringbot.datastorage

/**
  * Created by amir.
  */
case class Notification(stock: String,
                        price: Double,
                        notificationType: NotificationType,
                        userId: Long) extends Ordered[Notification] {
  override def compare(that: Notification): Int = price.compare(that.price) match {
    case 0 => userId.compare(that.userId)
    case x => x
  }
}

object Notification {
  def apply(price: Double): Notification = {
    Notification("", price, RaiseNotification, 0)
  }
}

sealed trait NotificationType

case object RaiseNotification extends NotificationType

case object FallNotification extends NotificationType

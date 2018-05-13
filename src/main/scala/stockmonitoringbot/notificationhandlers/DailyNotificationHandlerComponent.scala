package stockmonitoringbot.notificationhandlers

import stockmonitoringbot.datastorage.models.DailyNotification

/**
  * Created by amir.
  */

trait DailyNotificationHandlerComponent {
  val dailyNotificationHandler: DailyNotificationHandler
}

trait DailyNotificationHandler {
  def addDailyNotification(notification: DailyNotification): Unit
  def deleteDailyNotification(notificationId: Long): Unit
}

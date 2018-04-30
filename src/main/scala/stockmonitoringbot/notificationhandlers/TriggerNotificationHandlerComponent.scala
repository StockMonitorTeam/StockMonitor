package stockmonitoringbot.notificationhandlers

/**
  * Created by amir.
  */
trait TriggerNotificationHandlerComponent {
  val triggerNotificationHandler: TriggerNotificationHandler
}

trait TriggerNotificationHandler {
  def start(): Unit
  def stop(): Boolean
}

package stockmonitoringbot.messengerservices.markups

import info.mukel.telegrambot4s.models.{KeyboardButton, ReplyKeyboardMarkup}
import stockmonitoringbot.datastorage.{FallNotification, Notification, RaiseNotification}

object Buttons {

  val stock = "📈 Акции"
  val currency = "💰 Валюта"
  val collection = "💼 Мои портфели"
  val notifications = "⏱ Регулярные отчёты"
  val triggers = "🚨 События триггеры"
  val info = "❓ Информация"
  val backToMain = "⏮ Главное меню"

  val notificationGet = "⏱ Список оповещений"
  val notificationAdd = "➕ Новое оповещение"
  val notificationDel = "❌ Удалить оповещения"

}

object GeneralMarkups {

  def customKeyboard(keyboard : Seq[Seq[KeyboardButton]],
             resizeKeyboard : Option[Boolean] = Some(true),
             oneTimeKeyboard : Option[Boolean] = None,
             selective : Option[Boolean] = Some(true)): Option[ReplyKeyboardMarkup] = {
    Some(ReplyKeyboardMarkup(keyboard, resizeKeyboard, oneTimeKeyboard, selective))
  }

  val startMenuMarkup = customKeyboard(Seq(
      Seq(KeyboardButton(Buttons.stock), KeyboardButton(Buttons.currency)),
      Seq(KeyboardButton(Buttons.collection)),
      Seq(KeyboardButton(Buttons.notifications), KeyboardButton(Buttons.triggers)),
      Seq(KeyboardButton(Buttons.info))
    ))

  val stockMarkup = customKeyboard(Seq(
    Seq(KeyboardButton(Buttons.collection), KeyboardButton(Buttons.currency)),
    Seq(KeyboardButton(Buttons.backToMain))
  ))

  val notificationsMenuMarkup = customKeyboard(Seq(
    Seq(KeyboardButton(Buttons.notificationGet)),
    Seq(KeyboardButton(Buttons.notificationAdd)),
    Seq(KeyboardButton(Buttons.notificationDel))
  ))

  def notificationToString(notification: Notification): String = {
    val notificationType = notification.notificationType match {
      case RaiseNotification => ">"
      case FallNotification => "<"
    }
    s"${notification.stock} $notificationType ${notification.price}"
  }

  def notificationsMarkup(notifications: Seq[Notification]): Option[ReplyKeyboardMarkup] = Some(ReplyKeyboardMarkup.singleColumn(
    notifications.map(notification => KeyboardButton(notificationToString(notification))),
    resizeKeyboard = Some(true),
    oneTimeKeyboard = Some(true)))

}
package stockmonitoringbot.messengerservices.markups

import info.mukel.telegrambot4s.models.{InlineKeyboardButton, InlineKeyboardMarkup, KeyboardButton, ReplyKeyboardMarkup}
import stockmonitoringbot.datastorage.models.{FallNotification, Portfolio, RaiseNotification, TriggerNotification}
import stockmonitoringbot.messengerservices.CallbackTypes

object Buttons {

  val stock = "📈 Акции"
  val currency = "💰 Валюта"
  val portfolio = "💼 Мои портфели"
  val notifications = "⏱ Регулярные отчёты"
  val triggers = "🚨 События триггеры"
  val info = "❓ Информация"
  val backToMain = "⏮ Главное меню"

  val notificationGet = "⏱ Список оповещений"
  val notificationAdd = "➕ Новое оповещение"
  val notificationDel = "❌ Удалить оповещения"

  val portfolioCreate = "➕ Создать новый портфель"
  val portfolioDelete = "❌ Удалить портфель"

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
      Seq(KeyboardButton(Buttons.portfolio)),
      Seq(KeyboardButton(Buttons.notifications), KeyboardButton(Buttons.triggers)),
      Seq(KeyboardButton(Buttons.info))
    ))

  val stockMarkup = customKeyboard(Seq(
    Seq(KeyboardButton(Buttons.portfolio), KeyboardButton(Buttons.currency)),
    Seq(KeyboardButton(Buttons.backToMain))
  ))

  val portfolioMarkup = customKeyboard(Seq(
    Seq(KeyboardButton(Buttons.portfolioCreate)),
    Seq(KeyboardButton(Buttons.stock), KeyboardButton(Buttons.currency)),
    Seq(KeyboardButton(Buttons.notifications), KeyboardButton(Buttons.triggers)),
    Seq(KeyboardButton(Buttons.backToMain))
  ))

  val notificationsMenuMarkup = customKeyboard(Seq(
    Seq(KeyboardButton(Buttons.notificationGet)),
    Seq(KeyboardButton(Buttons.notificationAdd)),
    Seq(KeyboardButton(Buttons.notificationDel)),
    Seq(KeyboardButton(Buttons.backToMain))
  ))

  def notificationToString(notification: TriggerNotification): String = {
    val notificationType = notification.notificationType match {
      case RaiseNotification => ">"
      case FallNotification => "<"
    }
    //todo pattern match on stock/exchange rate/portfolio
    s"??? $notificationType ${notification.boundPrice}"
  }

  def notificationsMarkup(notifications: Seq[TriggerNotification]): Option[ReplyKeyboardMarkup] = Some(ReplyKeyboardMarkup.singleColumn(
    notifications.map(notification => KeyboardButton(notificationToString(notification))),
    resizeKeyboard = Some(true),
    oneTimeKeyboard = Some(true)))

  def generatePortfolioList(userId: Long, portfolios: Seq[Portfolio]): Option[InlineKeyboardMarkup] = Some(InlineKeyboardMarkup(
    portfolios.map(
        portfolio => InlineKeyboardButton(text = portfolio.name, callbackData = Some(s"${CallbackTypes.portfolio}_${userId}_${portfolio.name}"))
    ).grouped(3).toSeq
  ))


}
package stockmonitoringbot.messengerservices.markups

import info.mukel.telegrambot4s.models.{InlineKeyboardButton, InlineKeyboardMarkup, KeyboardButton, ReplyKeyboardMarkup}
import stockmonitoringbot.datastorage.models._
import stockmonitoringbot.messengerservices.CallbackTypes

object Buttons {

  val stock = "📈 Акции"
  val currency = "💰 Валюта"
  val portfolio = "💼 Мои портфели"
  val notifications = "⏱ Регулярные отчёты"
  val triggers = "🚨 События триггеры"
  val info = "❓ Информация"
  val backToMain = "⏮ Главное меню"
  val back = "⏪ Назад"

  val notificationGet = "⏱ Список оповещений"
  val notificationAdd = "➕ Новое оповещение"
  val notificationDel = "❌ Удалить оповещения"

  val portfolioCreate = "➕ Создать новый портфель"
  val portfolioDelete = "❌ Удалить портфель"

  val portfolioStockAdd = "➕ Добавить акцию"
  val portfolioStockDelete = "🗑 Удалить акцию"

  val notificationReject = "Отменить оповещения"

  val triggerAdd = "🚨 Установить триггер"
  val triggerRemove = "🗑 Удалить триггер"

}

object Inline {

  def generatePrefix(prefix: String, userId: Long, data: String) =
    s"${prefix}_${userId}_${data}"

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

  val viewPortfolioMarkup = customKeyboard(Seq(
    Seq(KeyboardButton(Buttons.portfolio)),
    Seq(KeyboardButton(Buttons.portfolioStockAdd), KeyboardButton(Buttons.portfolioStockDelete)),
    Seq(KeyboardButton(Buttons.notifications), KeyboardButton(Buttons.triggers)),
    Seq(KeyboardButton(Buttons.backToMain)),
    Seq(KeyboardButton(Buttons.portfolioDelete))
  ))

  val notificationsMenuMarkup = customKeyboard(Seq(
    Seq(KeyboardButton(Buttons.notificationGet)),
    Seq(KeyboardButton(Buttons.notificationAdd)),
    Seq(KeyboardButton(Buttons.notificationDel)),
    Seq(KeyboardButton(Buttons.backToMain))
  ))

  val portfolioTriggerMenuMarkup = customKeyboard(Seq(
    Seq(KeyboardButton(Buttons.triggerAdd), KeyboardButton(Buttons.triggerRemove)),
    Seq(KeyboardButton(Buttons.back)),
//    Seq(KeyboardButton(Buttons.portfolioStockAdd), KeyboardButton(Buttons.portfolioStockDelete)),
    Seq(KeyboardButton(Buttons.backToMain))
  ))

  val basicBackMarkup = customKeyboard(Seq(
    Seq(KeyboardButton(Buttons.back), KeyboardButton(Buttons.portfolio))
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

  def generatePortfolioNotificationOptions(userId: Long, portfolio: Portfolio): Option[InlineKeyboardMarkup] = Some(InlineKeyboardMarkup(
    Seq(
      Seq(
        InlineKeyboardButton(text="09:00", callbackData=Some(Inline.generatePrefix(CallbackTypes.portfolioSetNotification, userId, "09:00"))),
        InlineKeyboardButton(text="18:00", callbackData=Some(Inline.generatePrefix(CallbackTypes.portfolioSetNotification, userId, "18:00"))),
        InlineKeyboardButton(text="22:00", callbackData=Some(Inline.generatePrefix(CallbackTypes.portfolioSetNotification, userId, "22:00")))
      ),
      Seq(
        InlineKeyboardButton(text=Buttons.notificationReject, callbackData=Some(Inline.generatePrefix(CallbackTypes.portfolioSetNotification, userId, Buttons.notificationReject)))
      )
    )
  ))

  def generatePortfolioTriggersDelete(userId: Long, triggers: Seq[PortfolioTriggerNotification]): Option[InlineKeyboardMarkup] = Some(InlineKeyboardMarkup(
    triggers.map { x => {
      val triggerUniqueName = s"${x.notificationType} - ${x.boundPrice}"
      InlineKeyboardButton(text=triggerUniqueName, callbackData=Some(Inline.generatePrefix(CallbackTypes.portfolioDeleteTrigger, userId, triggerUniqueName)))
    }
    }.toSeq.grouped(3).toSeq
  ))

  def generatePortfolioStockDelete(userId: Long, portfolio: Portfolio): Option[InlineKeyboardMarkup] = Some(InlineKeyboardMarkup(
    portfolio.stocks.map {
        case (name, quantity) =>
          InlineKeyboardButton(text=name, callbackData=Some(Inline.generatePrefix(CallbackTypes.portfolioDeleteStock, userId, name)))
      }.toSeq.grouped(3).toSeq
  ))

  def generatePortfolioTriggerOptions(userId: Long, portfolio: Portfolio): Option[InlineKeyboardMarkup] = Some(InlineKeyboardMarkup(
    Seq(
      Seq(
        InlineKeyboardButton(text="Выше порога 📈", callbackData=Some(Inline.generatePrefix(CallbackTypes.triggerSetType, userId, RaiseNotification.toString))),
        InlineKeyboardButton(text="Ниже порога 📉", callbackData=Some(Inline.generatePrefix(CallbackTypes.triggerSetType, userId, FallNotification.toString))),
      ),
      Seq(
        InlineKeyboardButton(text="Обе стороны 📈➕📉", callbackData=Some(Inline.generatePrefix(CallbackTypes.triggerSetType, userId, BothNotification.toString)))
      )
    )
  ))

}
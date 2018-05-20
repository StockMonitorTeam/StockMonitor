package stockmonitoringbot.messengerservices.markups

import info.mukel.telegrambot4s.models.{InlineKeyboardButton, InlineKeyboardMarkup, KeyboardButton, ReplyKeyboardMarkup}
import stockmonitoringbot.datastorage.models._
import stockmonitoringbot.messengerservices.useractor.UserActor.CallbackTypes

object Buttons {

  val stock = "📈 Акции"
  val currency = "💰 Валюта"
  val portfolio = "💼 Мои портфели"
  val notifications = "⏱ Регулярные отчёты"
  val triggers = "🚨 События триггеры"
  val info = "❓ Информация"
  val settings = "⚙️ Настройки"
  val backToMain = "⏮ Главное меню"
  val back = "⏪ Назад"

  val notificationGet = "⏱ Список оповещений"
  val notificationAdd = "➕ Новое оповещение"
  val notificationDel = "❌ Удалить оповещения"

  val portfolioCreate = "➕ Создать новый портфель"
  val portfolioDelete = "❌ Удалить портфель"

  val timezone = "⏲ Редактировать часовой пояс"

  val portfolioStockAdd = "➕ Добавить акцию"
  val portfolioStockDelete = "🗑 Удалить акцию"

  val notificationReject = "Отменить оповещения"

  val triggerAdd = "🚨 Установить триггер"
  val triggerRemove = "🗑 Удалить триггер"

  val timezoneChange = "⏲ Изменить часовой пояс"

}

object Inline {

  def generatePrefix(prefix: String, userId: Long, data: String) =
    s"${prefix}_${userId}_${data}"

}

object GeneralMarkups {

  def customKeyboard(keyboard: Seq[Seq[KeyboardButton]],
                     resizeKeyboard: Option[Boolean] = Some(true),
                     oneTimeKeyboard: Option[Boolean] = None,
                     selective: Option[Boolean] = Some(true)): Option[ReplyKeyboardMarkup] = {
    Some(ReplyKeyboardMarkup(keyboard, resizeKeyboard, oneTimeKeyboard, selective))
  }

  val startMenuMarkup = customKeyboard(Seq(
    Seq(KeyboardButton(Buttons.stock), KeyboardButton(Buttons.currency)),
    Seq(KeyboardButton(Buttons.portfolio)),
    //      Seq(KeyboardButton(Buttons.notifications), KeyboardButton(Buttons.triggers)),
    Seq(KeyboardButton(Buttons.settings))
  ))

  val onlyMainMenu = customKeyboard(Seq(
    Seq(KeyboardButton(Buttons.backToMain))
  ))

  val onlyBack = customKeyboard(Seq(
    Seq(KeyboardButton(Buttons.back))
  ))

  val settingsMenuMarkup = customKeyboard(Seq(
    Seq(KeyboardButton(Buttons.notifications), KeyboardButton(Buttons.triggers)),
    Seq(KeyboardButton(Buttons.timezone)),
    Seq(KeyboardButton(Buttons.info)),
    Seq(KeyboardButton(Buttons.backToMain))
  ))

  val triggersMenuMarkup = customKeyboard(Seq(
    Seq(KeyboardButton(Buttons.triggerRemove)),
    Seq(KeyboardButton(Buttons.settings)),
    Seq(KeyboardButton(Buttons.backToMain))
  ))

  val dailyNotificationsMenuMarkup = customKeyboard(Seq(
    Seq(KeyboardButton(Buttons.notificationDel)),
    Seq(KeyboardButton(Buttons.settings)),
    Seq(KeyboardButton(Buttons.backToMain))
  ))

  val timezoneMenuMarkup = customKeyboard(Seq(
    Seq(KeyboardButton(Buttons.timezoneChange)),
    Seq(KeyboardButton(Buttons.settings)),
    Seq(KeyboardButton(Buttons.backToMain))
  ))

  val oneStockMenuMarkup = customKeyboard(Seq(
    Seq(KeyboardButton(Buttons.notificationAdd), KeyboardButton(Buttons.triggerAdd)),
    Seq(KeyboardButton(Buttons.stock)),
    Seq(KeyboardButton(Buttons.backToMain))
  ))

  val oneExchangeRateMenuMarkup = customKeyboard(Seq(
    Seq(KeyboardButton(Buttons.notificationAdd), KeyboardButton(Buttons.triggerAdd)),
    Seq(KeyboardButton(Buttons.currency)),
    Seq(KeyboardButton(Buttons.backToMain))
  ))

  val portfolioMarkup = customKeyboard(Seq(
    Seq(KeyboardButton(Buttons.portfolioCreate)),
    //    Seq(KeyboardButton(Buttons.stock), KeyboardButton(Buttons.currency)),
    //Seq(KeyboardButton(Buttons.notifications), KeyboardButton(Buttons.triggers)),
    Seq(KeyboardButton(Buttons.backToMain))
  ))

  val viewPortfolioMarkup = customKeyboard(Seq(
    Seq(KeyboardButton(Buttons.portfolioStockAdd), KeyboardButton(Buttons.portfolioStockDelete)),
    Seq(KeyboardButton(Buttons.notifications), KeyboardButton(Buttons.triggers)),
    Seq(KeyboardButton(Buttons.portfolio)),
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

  def portfolioCurrencySwitch(userId: Long) = Some(InlineKeyboardMarkup(
    Seq(Seq(
      InlineKeyboardButton(text = "USD", callbackData = Some(Inline.generatePrefix(CallbackTypes.choseCurrency, userId, "USD"))),
      InlineKeyboardButton(text = "RUB", callbackData = Some(Inline.generatePrefix(CallbackTypes.choseCurrency, userId, "RUB"))),
      InlineKeyboardButton(text = "EUR", callbackData = Some(Inline.generatePrefix(CallbackTypes.choseCurrency, userId, "EUR")))
    ))
  ))

  def generatePortfolioList(userId: Long, portfolios: Seq[Portfolio]): Option[InlineKeyboardMarkup] = Some(InlineKeyboardMarkup(
    portfolios.map(
      portfolio => InlineKeyboardButton(text = portfolio.name, callbackData = Some(s"${CallbackTypes.portfolio}_${userId}_${portfolio.name}"))
    ).grouped(2).toSeq
  ))

  def generateDailyNotificationOptions(userId: Long): Option[InlineKeyboardMarkup] = Some(InlineKeyboardMarkup(
    Seq(
      Seq(
        InlineKeyboardButton(text = "09:00", callbackData = Some(Inline.generatePrefix(CallbackTypes.notificationTime, userId, "09:00"))),
        InlineKeyboardButton(text = "18:00", callbackData = Some(Inline.generatePrefix(CallbackTypes.notificationTime, userId, "18:00"))),
        InlineKeyboardButton(text = "22:00", callbackData = Some(Inline.generatePrefix(CallbackTypes.notificationTime, userId, "22:00")))
      ),
      Seq(
        InlineKeyboardButton(text = Buttons.notificationReject, callbackData = Some(Inline.generatePrefix(CallbackTypes.notificationTime, userId, Buttons.notificationReject)))
      )
    )
  ))

  def generatePortfolioTriggersDelete(userId: Long, triggers: Seq[TriggerNotification]): Option[InlineKeyboardMarkup] = Some(InlineKeyboardMarkup(
    triggers.map { x => {
      val triggerName = s"${x.notificationType} - ${x.boundPrice}"
      val triggerUniqueName = x.id.toString
      InlineKeyboardButton(text = triggerName, callbackData = Some(Inline.generatePrefix(CallbackTypes.portfolioDeleteTrigger, userId, triggerUniqueName)))
    }
    }.grouped(3).toSeq
  ))

  def generateTriggersDelete(userId: Long, triggers: Seq[TriggerNotification]): Option[InlineKeyboardMarkup] = Some(InlineKeyboardMarkup(
    triggers.map { x => {
      val triggerType = x match {
        case StockTriggerNotification(_, _, stock, _, _) => s"акции $stock"
        case ExchangeRateTriggerNotification(_, _, (from, to), _, _) => s"курс $from/$to"
        case PortfolioTriggerNotification(_, _, portfolio, _, _) => s"портфель $portfolio"
      }
      val triggerName = s"$triggerType ${x.notificationType} - ${x.boundPrice}"
      val triggerUniqueName = x.id.toString
      InlineKeyboardButton(text = triggerName, callbackData = Some(Inline.generatePrefix(CallbackTypes.deleteTrigger,
        userId, triggerUniqueName)))
    }
    }.grouped(3).toSeq
  ))

  def generateDailyNotificationsDelete(userId: Long, notifications: Seq[DailyNotification]): Option[InlineKeyboardMarkup] = Some(InlineKeyboardMarkup(
    notifications.map { x => {
      val notType = x match {
        case StockDailyNotification(_, _, stock, _) => s"акции $stock"
        case ExchangeRateDailyNotification(_, _, (from, to), _) => s"курс $from/$to"
        case PortfolioDailyNotification(_, _, portfolio, _) => s"портфель $portfolio"
      }
      val notName = s"$notType в ${x.time}"
      val notUniqueName = x.id.toString
      InlineKeyboardButton(text = notName, callbackData = Some(Inline.generatePrefix(CallbackTypes.deleteDailyNot,
        userId, notUniqueName)))
    }
    }.grouped(3).toSeq
  ))

  def generatePortfolioStockDelete(userId: Long, portfolio: Portfolio): Option[InlineKeyboardMarkup] = Some(InlineKeyboardMarkup(
    portfolio.stocks.map {
      case (name, _) =>
        InlineKeyboardButton(text = name, callbackData = Some(Inline.generatePrefix(CallbackTypes.portfolioDeleteStock, userId, name)))
    }.toSeq.grouped(3).toSeq
  ))

  def generateTriggerOptions(userId: Long): Option[InlineKeyboardMarkup] = Some(InlineKeyboardMarkup(
    Seq(
      Seq(
        InlineKeyboardButton(text = "Выше порога 📈", callbackData = Some(Inline.generatePrefix(CallbackTypes.triggerSetType, userId, Raise.toString))),
        InlineKeyboardButton(text = "Ниже порога 📉", callbackData = Some(Inline.generatePrefix(CallbackTypes.triggerSetType, userId, Fall.toString))),
      ),
      Seq(
        InlineKeyboardButton(text = "Обе стороны 📈➕📉", callbackData = Some(Inline.generatePrefix(CallbackTypes.triggerSetType, userId, Both.toString)))
      )
    )
  ))

}
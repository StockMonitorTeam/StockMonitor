package stockmonitoringbot.messengerservices.markups

import info.mukel.telegrambot4s.models.{KeyboardButton, ReplyKeyboardMarkup}
import stockmonitoringbot.datastorage.{FallNotification, Notification, RaiseNotification}


object GeneralTexts {

  val INTRO_MESSAGE =
    """
      |StockMon бот умеет показывать информацию о стоимости акций, позволяет создавать вам свои портфели и контролировать их стоимость при помощи регулярных отчётов и установки оповещений при росте и падении выше порога.
      |
      |Для использования выберите команду в панели меню бота или введите /help.
    """.stripMargin

  val MAIN_MENU_GREETING = "Для продолжения работы выберите команду в панели меню бота или введите /help."

  val STOCK_INTRO_MESSAGE =
    """
      |Для получения информации о стоимости акций введите её тикер как текст (YNDX) или как команду (/YNDX).
      |
      |Примеры популярных тикеров:
      |/AMZN – Amazon.com, Inc
      |/GAZP – Gazprom PAO
      |/YNDX – Yandex N.V
    """.stripMargin

  val printStockPrice = (name: String, price: Double) =>
    s"""
      |Акции $name
      |Стоимость: $price
    """.stripMargin

  val printStockException = (name: String) =>
    s"Ошибка получения информации об акции: $name"

  val UNIMPLEMENTED = "В стадии разработки. Приносим извинения за неудобства. 😌"

}
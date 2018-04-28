package stockmonitoringbot.messengerservices.markups

import stockmonitoringbot.datastorage.models.Portfolio

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

  val PORTFOLIO_GREETING = (portfolios: String) =>
    s"""
       |Портфель – это список ваших акций. Вы сможете следить за суммарной стоимостью акций в портфеле и настраивать оповещения.
       |
       |Ваши портфели:
       |$portfolios
    """.stripMargin

  val PORTFOLIO_LIST = "Ваши портфели:"

  val NO_PORTFOLIO_GREETING =
    s"""
       |Портфель – это список ваших акций. Вы сможете следить за суммарной стоимостью акций в портфеле и настраивать оповещения.
       |
       |Для добавления выберите соответствующий пункт в меню бота или нажмите /createWatchlist
    """.stripMargin

  val INPUT_PORTFOLIO_NAME = "Введите название нового портфеля"
  val INPUT_PORTFOLIO_NAME_INVALID = "Введите пожалуйста название, содержащее только латинские буквы," +
    " цифры или знаки тире, подчеркивания, пробел."
  val INPUT_PORTFOLIO_CURRENCY = (name: String) =>
    s"Выберите валюту для портфеля «$name» из списка ниже либо введите её название."
  val INPUT_PORTFOLIO_CURRENCY_INVALID = "Введите пожалуйста одну из следующих валют: USD, EUR, RUB"
  val INPUT_PORTFOLIO_CREATED = (name: String, currency: String) =>
    s"Портфель «$name» с валютой $currency успешно создан."
  val PORTFOLIO_CREATE_ERROR = s"Ошибка добавления портфеля"

  val PORTFOLIO_SHOW = (portfolio: Portfolio) =>
    s"""
       |Портфель «${portfolio.name}»
       |Валюта: ${portfolio.currency}
     """.stripMargin

}
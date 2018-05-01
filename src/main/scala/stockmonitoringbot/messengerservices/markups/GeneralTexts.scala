package stockmonitoringbot.messengerservices.markups

import stockmonitoringbot.datastorage.models.{Portfolio, PortfolioDailyNotification}

object GeneralTexts {

  val INTRO_MESSAGE =
    """StockMon бот умеет показывать информацию о стоимости акций, позволяет создавать вам свои портфели и контролировать их стоимость при помощи регулярных отчётов и установки оповещений при росте и падении выше порога.
      |
      |Для использования выберите команду в панели меню бота или введите /help.
    """.stripMargin

  val MAIN_MENU_GREETING = "Для продолжения работы выберите команду в панели меню бота или введите /help."

  val STOCK_INTRO_MESSAGE =
    """Для получения информации о стоимости акций введите её тикер как текст (YNDX) или как команду (/YNDX).
      |
      |Примеры популярных тикеров:
      |/AMZN – Amazon.com, Inc
      |/GAZP – Gazprom PAO
      |/YNDX – Yandex N.V
    """.stripMargin

  val printStockPrice = (name: String, price: Double) =>
    s"""Акции $name
       |Стоимость: $price
    """.stripMargin

  val printStockException = (name: String) =>
    s"Ошибка получения информации об акции: $name"

  val UNIMPLEMENTED = "В стадии разработки. Приносим извинения за неудобства. 😌"

  val PORTFOLIO_HELLO = "Портфель – это список ваших акций. Вы сможете следить за суммарной стоимостью акций в портфеле и настраивать оповещения."

  val PORTFOLIO_GREETING = (portfolios: String) =>
    s"""Портфель – это список ваших акций. Вы сможете следить за суммарной стоимостью акций в портфеле и настраивать оповещения.
       |
       |Ваши портфели:
       |$portfolios
    """.stripMargin

  val PORTFOLIO_LIST = "Ваши портфели:"

  val NO_PORTFOLIO_GREETING =
    s"""Портфель – это список ваших акций. Вы сможете следить за суммарной стоимостью акций в портфеле и настраивать оповещения.
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

  val PORTFOLIO_SHOW = (portfolio: Portfolio, price: BigDecimal) =>
    s"""Портфель «${portfolio.name}»
       |Валюта: ${portfolio.currency}
       |Стоимость портфеля: ${price}
       |""".stripMargin

  val PORTFOLIO_SHOW_STOCK = "Акции в портфеле:\n"

  val PORTFOLIO_STOCK_ADD = (name: String) =>
    s"""Для добавления новой акции в портфель «${name}» введите её тикер как текст (YNDX) или как команду (/YNDX).
      |
      |Будьте внимательны с валютой, в которой торгуется данная акция. Мы пока не умеем автоматически её определять. Вы можете создать несколько портфелей для акций, торгуемых в разных валютах.
    """.stripMargin

  val PORTFOLIO_STOCK_ADD_ERROR = s"Ошибка добавления акции в портфель"

  val PORTFOLIO_STOCK_ADD_QUERY = s"Данный тикер не найден в системе. Пытаемся запросить. Пожалуйста, подождите 😌"

  val PORTFOLIO_STOCK_ADD_AMOUNT = (ticker: String, portfolioName: String) =>
    s"Для добавления $ticker в портфель «$portfolioName» введите количество акций. Например: 1 или 0.03"

  val PORTFOLIO_DAILY_NOTIFICATION = (portfolioName: String, notification: Option[PortfolioDailyNotification]) =>
    s"""Для того, чтобы задать ежедневное оповещение о стоимости портфеля «${portfolioName}» выберите время, либо введите его в формате HH:MM.
      |
      |На текущий момент у вас""".stripMargin +
      (notification match {
        case Some(x) => s" установлены оповещения на ${x.time.toString}"
        case None => " не установлены оповещения"
      })

  val PORTFOLIO_DAILY_NOTIFICATION_SET = (time: String) => s"Оповещение установлено на ${time}"

  val PORTFOLIO_DAILY_NOTIFICATION_UNSET = "Оповещения удалены"

  val PORTFOLIO_STOCK_DELETE = (name: String) => s"Для удаления акции из портфеля «${name}» нажмите на соответствующую кнопку"

  val PORTFOLIO_STOCK_DELETE_SUCCESS = (stock: String, name: String) => s"Акция ${stock} успешно удалена из портфеля «${name}»"

  val PORTFOLIO_STOCK_DELETE_FAIL = (stock: String, name: String) => s"Не удалось удалить акцию ${stock} из портфеля «${name}»"

  val PORTFOLIO_STOCK_EMPTY = (name: String) => s"В портфеле ${name} нет акций"

  val TIME_ERROR = "Неверно задан формат времени"

  val ERROR = "Произошла ошибка 🙁. Будем благодарны, если вы сообщите подробности разработчикам."

  val PORTFOLIO_TRIGGERS = (name: String, price: BigDecimal) =>
    s"""Для того, чтобы задать оповещение о резком изменении стоимости портфеля «${name}» введите интересующий вас порог.
      |
      |Текущая стоимость портфеля ➔ ${price}
    """.stripMargin
}
package stockmonitoringbot.messengerservices.markups

import stockmonitoringbot.datastorage.models._
import stockmonitoringbot.stockpriceservices.{CurrencyExchangeRateInfo, StockInfo}

object GeneralTexts {

  val INTRO_MESSAGE =
    """StockMon бот умеет показывать информацию о стоимости акций, позволяет создавать вам свои портфели и контролировать их стоимость при помощи регулярных отчётов и установки оповещений при росте и падении выше порога.
      |
      |Для использования выберите команду в панели меню бота.
    """.stripMargin

  val MAIN_MENU_GREETING = "Для продолжения работы выберите команду в панели меню бота."

  val STOCK_INTRO_MESSAGE =
    """Для получения информации о стоимости акций введите её тикер как текст (YNDX) или как команду (/YNDX).
      |
      |Примеры популярных тикеров:
      |/AMZN – Amazon, Inc
      |/GAZP – Gazprom PAO
      |/YNDX – Yandex N.V
      |/AAPL – Apple Inc.
      |
      |Больше примеров тикеров по адресу https://goo.gl/s7pnNS
    """.stripMargin

  val EXCHANGE_RATE_INTRO_MESSAGE =
    """Для получения информации о курсе валют введите пару через "/".
      |
      |Например: USD/RUB
      |
    """.stripMargin

  val EXCHANGE_RATE_INVALID = "Ваше сообщение не соответствует формату"

  val SETTINGS_INTRO_MESSAGE =
    """Настройки вашего профиля. Здесь вы можете отредактировать ваши уведомления и часовой пояс
      |
    """.stripMargin

  val INFO_MESSAGE =
    """О всех проблемах или пожеланиях пишите на этот email: XXXXXXXXXX.
      |Данные взяты с сайта: https://www.alphavantage.co/
      |
    """.stripMargin

  val printStockPrice = (name: String, price: Double,
                         triggerNot: Seq[TriggerNotification],
                         dailyNot: Option[DailyNotification]) => {
    val dailyNotStr = dailyNot.fold("Ежедневное оповещение не установлено") { not =>
      s"Ежедневное оповещение о $name установлено на: ${not.time}"
    }
    s"""Акции $name
       |Стоимость: $price
       |Подробнее: https://www.marketwatch.com/investing/stock/$name
       |
       |Активные оповещения тригеры на $name:
       |${triggerNot.map(tnToStr).mkString("\n")}
       |
       |$dailyNotStr
    """.stripMargin
  }

  val printExchangeRate = (rate: CurrencyExchangeRateInfo,
                           triggerNot: Seq[TriggerNotification],
                           dailyNot: Option[DailyNotification]) => {
    val dailyNotStr = dailyNot.fold("Ежедневное оповещение не установлено") { not =>
      s"Ежедневное оповещение о курсе ${rate.from}/${rate.to} установлено на: ${not.time}"
    }
    s"""Курс ${rate.from} к ${rate.to} равен ${rate.rate}
       |
       |Активные оповещения тригеры на курс ${rate.from}/${rate.to}:
       |${triggerNot.map(tnToStr).mkString("\n")}
       |
       |$dailyNotStr
     """.stripMargin
  }

  val printStockException = (name: String) =>
    s"Ошибка получения информации об акции: $name 😔"

  val printExchangeRateException = (from: String, to: String) =>
    s"Ошибка получения информации о курсе: $from/$to 😔"

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
       |Для добавления выберите пункт «Создать новый портфель» в меню бота
    """.stripMargin

  val INPUT_PORTFOLIO_NAME = "Введите название нового портфеля (Латинские буквы, цифры, пробелы)"
  val INPUT_PORTFOLIO_NAME_INVALID = "Введите пожалуйста название, содержащее только латинские буквы," +
    " цифры или знаки тире, подчеркивания, пробел. Не менее 3 символов."
  val INPUT_PORTFOLIO_NAME_EXISTS = "У вас уже существует портфель с таким названием. Выберите другое."
  val INPUT_PORTFOLIO_CURRENCY = (name: String) =>
    s"Введите валюту для портфеля «$name»."
  val INPUT_PORTFOLIO_CURRENCY_LIST = "Вы можете выбрать валюту из списка"
  val INPUT_PORTFOLIO_CURRENCY_INVALID = "Введите пожалуйста одну из следующих валют: USD, EUR, RUB"
  val INPUT_PORTFOLIO_CREATED = (name: String, currency: String) =>
    s"Портфель «$name» с валютой $currency успешно создан."
  val PORTFOLIO_CREATE_ERROR = s"Ошибка добавления портфеля 😐"

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

  val DAILY_NOTIFICATION_ADD_INFO_INTRO = (assetType: AssetType) => {
    val asset = assetType match {
      case PortfolioAsset(name) => s"стоимости портфеля $name"
      case StockAsset(name) => s"стоимости акций $name"
      case ExchangeRateAsset(from, to) => s"курсе валют $from/$to"
    }
    s"Для того, чтобы задать ежедневное оповещение о $asset выберите время, либо введите его в формате HH:MM."
  }

  val DAILY_NOTIFICATION_ADD_INFO = (notification: Option[DailyNotification]) => {
    s"""
      |На текущий момент у вас """.stripMargin +
      (notification match {
        case Some(x) => s"установлены оповещения на ${x.time.toString}"
        case None => "не установлены оповещения"
      })
  }

  val DAILY_NOTIFICATION_SET = (time: String) => s"Оповещение установлено на ${time}"

  val DAILY_NOTIFICATION_UNSET = "Оповещения удалены"

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
       |
      |На данный момент у вас установлены оповещения:
       |""".stripMargin

  val PORTFOLIO_TRIGGERS_LIST = (triggers: Seq[TriggerNotification]) => triggers match {
    case Nil => """
                  |ни одного 🤨
                """.stripMargin
    case xList => xList map (x => s"🔈 на ${x.boundPrice} (${x.notificationType})") mkString "\n"
  }

  val TRIGGERS_LIST = (triggers: Seq[TriggerNotification]) => "Ваши активные тригеры: \n" + (triggers match {
    case Seq() => """
                    |Ни одного.
                  """.stripMargin
    case xList => xList.map(tnToStr).mkString("\n")
  })

  val DAILY_NOTIFICATIONS_LIST = (not: Seq[DailyNotification]) => "Ваши ежедневные оповещения: \n" + (not match {
    case Seq() => """
                    |Ни одного.
                  """.stripMargin
    case xList => xList.map(dnToStr).mkString("\n")
  })

  val TRIGGER_TEXT_ACTIVE = "Ваши активные тригеры:"

  val PORTFOLIO_TRIGGER_REMOVE = "Выберите триггер, который желаете удалить"

  val TRIGGER_REMOVE = "Выберите триггер, который желаете удалить"

  val DAILY_NOTIFICATION_REMOVE = "Выберите оповещение, которое желаете удалить"

  val DAILY_NOTIFICATION_TEXT_ACTIVE = "Ваши оповещения:"

  val PORTFOLIO_TRIGGER_EMPTY = "Список триггеров пуст 📭"

  val TRIGGER_TYPE = "Выберите тип срабатывания триггера при преодолении порога"

  val TRIGGER_TYPE_MORE = "В зависимости от типа, вы будете получать сообщение при подъеме или падении цены портфеля до определенного уровня"

  val TRIGGER_BOUND = "Введите порог срабатывания. Например: 1 или 133.7"

  val TRIGGER_ADDED = "Триггер успешно установлен"

  val TRIGGER_ADD_ERROR = "Триггер успешно установлен"

  val PORTFOLIO_TRIGGER_REMOVED = (name: String) => s"Триггер ${name} успешно удалён"

  val TRIGGER_REMOVED = s"Триггер успешно удалён"

  val DAILY_NOTIFICATION_REMOVED = s"Триггер успешно удалён"

  val DAILY_NOTIFICATION_STOCK_INFO = (stockInfo: StockInfo) => s"Цена акции ${stockInfo.name} : ${stockInfo.price}(обновлено: ${stockInfo.lastRefreshed})"

  val DAILY_NOTIFICATION_EXCHANGE_RATE_INFO = (exchangeRateInfo: CurrencyExchangeRateInfo) => s"Цена валютной пары ${exchangeRateInfo.from}/${exchangeRateInfo.to} : ${exchangeRateInfo.rate}" +
    s"(обновлено: ${exchangeRateInfo.lastRefreshed})" +
    s"\n ${exchangeRateInfo.from} - ${exchangeRateInfo.descriptionFrom}" +
    s"\n ${exchangeRateInfo.to} - ${exchangeRateInfo.descriptionTo}"

  val DAILY_NOTIFICATION_PORTFOLIO_INFO = (name: String, price: BigDecimal) => s"Цена вашего портфеля «$name» : $price"

  val TRIGGER_MESSAGE_BOUND = (tp: TriggerNotificationType, bound: BigDecimal) => tp match {
    case RaiseNotification =>
      s"поднялась выше $bound"
    case FallNotification =>
      s"опустилась ниже $bound"
    case BothNotification =>
      s"достигла порога: $bound"
  }

  val TRIGGER_MESSAGE = (notification: TriggerNotification, price: BigDecimal) => notification match {
    case StockTriggerNotification(_, stock, bound, notificationType) =>
      val msg = TRIGGER_MESSAGE_BOUND(notificationType, bound)
      s"Сработало триггер оповещение! Стоимость $stock $msg. Текущая цена $price"
    case ExchangeRateTriggerNotification(_, (from, to), bound, notificationType) =>
      val msg = TRIGGER_MESSAGE_BOUND(notificationType, bound)
      s"Сработало триггер оповещение! Цена валютной пары $from/$to $msg. Текущая цена: $price"
    case PortfolioTriggerNotification(_, portfolioName, bound, notificationType) =>
      val msg = TRIGGER_MESSAGE_BOUND(notificationType, bound)
      s"Сработало триггер оповещение! Стоимость портеля «$portfolioName» $msg. Текущая цена: $price"
  }


}
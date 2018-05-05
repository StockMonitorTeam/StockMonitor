package stockmonitoringbot.messengerservices.useractor

import java.time.LocalTime
import java.time.format.DateTimeFormatter

import akka.actor.Actor
import akka.actor.Actor.Receive
import stockmonitoringbot.datastorage.models._
import stockmonitoringbot.messengerservices.markups.{Buttons, GeneralMarkups, GeneralTexts}
import stockmonitoringbot.messengerservices.useractor.UserActor.{CallbackTypes, IncomingCallback, IncomingMessage, SetBehavior}
import stockmonitoringbot.notificationhandlers.{getPortfolioCurrentPrice, getPortfolioStocksPrice}

import scala.util.{Failure, Success}

/**
  * Created by amir.
  */
trait Portfolios {
  this: MainStuff =>
  /*
  def becomePortfolioMainMenu(): Unit = {
    userDataStorage.getUserPortfolios(userId).onComplete {
      case Success(results) => results match {
        case Seq() =>
          sendMessageToUser(GeneralTexts.NO_PORTFOLIO_GREETING, GeneralMarkups.portfolioMarkup)
          context become waitForPortfolio
        case portfolios =>
          sendMessageToUser(GeneralTexts.PORTFOLIO_HELLO, GeneralMarkups.portfolioMarkup)
          sendInlineMessageToUser(GeneralTexts.PORTFOLIO_LIST, GeneralMarkups.generatePortfolioList(userId, portfolios))
          context become waitForPortfolio
      }
      case Failure(e) =>
        logger.error("Can't get portfolios", e)
        sendMessageToUser(GeneralTexts.ERROR)
    }
  }

  def printPortfolio(userId: Long, portfolioName: String): Unit = {
    for {
      portfolio <- userDataStorage.getPortfolio(userId, portfolioName)
      stockPrices <- getPortfolioStocksPrice(portfolio, cache)
    } {
      val sum = stockPrices.foldLeft(BigDecimal(0))(_ + _._2)
      val portfolioGreeting = GeneralTexts.PORTFOLIO_SHOW(portfolio, sum) +
        (
          if (portfolio.stocks.isEmpty)
            ""
          else
            GeneralTexts.PORTFOLIO_SHOW_STOCK +
              portfolio.stocks.map {
                case (k, v) =>
                  s"$k ($v) ➔ ${stockPrices(k)}"
              }.mkString("\n")
          )
      sendMessageToUser(portfolioGreeting, GeneralMarkups.viewPortfolioMarkup)
      context become portfolioMenu(portfolio)
    }
  }

  def printPortfolioTriggers(userId: Long, portfolio: Portfolio): Unit = {
    for {
      price <- getPortfolioCurrentPrice(portfolio, cache)
      triggers <- userDataStorage.getUserTriggerNotification(userId, PortfolioAsset(portfolio.name))
    } {
      val message = GeneralTexts.PORTFOLIO_TRIGGERS(portfolio.name, price) + GeneralTexts.PORTFOLIO_TRIGGERS_LIST(triggers)
      sendMessageToUser(message, GeneralMarkups.portfolioTriggerMenuMarkup)
    }
  }

  def clearPortfolioNotification(userId: Long, portfolio: Portfolio): Unit = {
    userDataStorage.getUserNotification(userId, PortfolioAsset(portfolio.name)).onComplete {
      case Success(Some(x)) =>
        dailyNotification.deleteDailyNotification(x)
        userDataStorage.deleteDailyNotification(x)
      case _ =>
    }
  }

  def setPortfolioNotification(userId: Long, portfolio: Portfolio, time: String): Unit = {
    try {
      val localTime: LocalTime = LocalTime.parse(time, DateTimeFormatter.ofPattern("H:mm"))
      val notification = PortfolioDailyNotification(userId, portfolio.name, localTime)

      clearPortfolioNotification(userId, portfolio)
      dailyNotification.addDailyNotification(notification)
      userDataStorage.addDailyNotification(notification)
      sendMessageToUser(GeneralTexts.DAILY_NOTIFICATION_SET(time))
    }
    catch {
      case e: Exception => sendMessageToUser(GeneralTexts.TIME_ERROR)
    }
  }

  // Telegram callback handlers
  def tgCallback: Receive = {
    case IncomingCallback(CallbackTypes.portfolio, data) =>
      printPortfolio(userId, data.message)
  }

  def waitForPortfolioTrigger(portfolio: Portfolio): Receive = {
    case IncomingMessage(Buttons.triggerAdd) => {
      sendInlineMessageToUser(GeneralTexts.TRIGGER_TYPE, GeneralMarkups.generateTriggerOptions(userId))
      context become waitForPortfolioTriggerAddType(portfolio)
    }
    case IncomingMessage(Buttons.triggerRemove) => {
      userDataStorage.getUserTriggerNotification(userId, PortfolioAsset(portfolio.name)) onComplete {
        case Success(Nil) =>
          sendMessageToUser(GeneralTexts.PORTFOLIO_TRIGGER_EMPTY)
        case Success(x) =>
          sendInlineMessageToUser(GeneralTexts.PORTFOLIO_TRIGGER_REMOVE, GeneralMarkups.generatePortfolioTriggersDelete(userId, x))
        case _ =>
          sendMessageToUser(GeneralTexts.ERROR)
      }
    }
    case IncomingMessage(Buttons.back) =>
      printPortfolio(userId, portfolio.name)
    case IncomingCallback(CallbackTypes.portfolioDeleteTrigger, message) => {
      message.message.split(" - ", 2) match {
        case Array(notificationType, boundPrice) =>
          val notification = PortfolioTriggerNotification(userId, portfolio.name, BigDecimal(boundPrice), TriggerNotificationType.define(notificationType))
          userDataStorage.deleteTriggerNotification(notification)
          sendMessageToUser(GeneralTexts.PORTFOLIO_TRIGGER_REMOVED(message.message))
        case _ =>
          sendMessageToUser(GeneralTexts.ERROR)
      }
    }
  }

  def waitForPortfolioTriggerAddType(portfolio: Portfolio): Receive = waitForPortfolioTrigger(portfolio) orElse {
    case IncomingCallback(CallbackTypes.triggerSetType, x) => {
      val nType = TriggerNotificationType.define(x.message)
      sendMessageToUser(GeneralTexts.TRIGGER_BOUND)
      context become waitForPortfolioTriggerAddBound(portfolio, nType)
    }
  }

  def waitForPortfolioTriggerAddBound(portfolio: Portfolio, nType: TriggerNotificationType): Receive = waitForPortfolioTrigger(portfolio) orElse {
    case IncomingMessage(floatAmount(bound)) => {
      val boundPrice = BigDecimal(bound)
      val notification = PortfolioTriggerNotification(userId, portfolio.name, boundPrice, nType)
      userDataStorage.addTriggerNotification(notification).onComplete {
        case Success(_) =>
          sendMessageToUser(GeneralTexts.TRIGGER_ADDED)
          printPortfolio(userId, portfolio.name)
        case _ =>
      }
      context become portfolioMenu(portfolio)
    }
  }

  def waitForPortfolioNotificationTime(portfolio: Portfolio): Receive = portfolioMenu(portfolio) orElse {

    case IncomingCallback(CallbackTypes.portfolioSetNotification, x) => x.message match {
      case Buttons.notificationReject => {
        clearPortfolioNotification(userId, portfolio)
        sendMessageToUser(GeneralTexts.PORTFOLIO_DAILY_NOTIFICATION_UNSET)
        context become portfolioMenu(portfolio)
      }
      case time: String => {
        setPortfolioNotification(userId, portfolio, time)
        context become portfolioMenu(portfolio)
      }
    }
    case IncomingMessage(time) => {
      setPortfolioNotification(userId, portfolio, time)
      context become portfolioMenu(portfolio)
    }

  }

  def waitForPortfolio: Receive = common orElse {
    case IncomingMessage(Buttons.portfolioCreate) => {
      sendMessageToUser(GeneralTexts.INPUT_PORTFOLIO_NAME)
      context become waitForPortfolioName
    }
  }

  def portfolioMenu(portfolio: Portfolio): Receive = common orElse {
    case IncomingMessage(Buttons.portfolioStockAdd) =>
      sendMessageToUser(GeneralTexts.PORTFOLIO_STOCK_ADD(portfolio.name), GeneralMarkups.viewPortfolioMarkup)
      context become waitForPortfolioStock(portfolio)
    case IncomingMessage(Buttons.portfolioStockDelete) => {
      if (portfolio.stocks.isEmpty) {
        sendMessageToUser(GeneralTexts.PORTFOLIO_STOCK_EMPTY(portfolio.name))
      } else {
        sendInlineMessageToUser(GeneralTexts.PORTFOLIO_STOCK_DELETE(portfolio.name), GeneralMarkups.generatePortfolioStockDelete(userId, portfolio))
        context become waitForPortfolioStock(portfolio)
      }
    }
    case IncomingMessage(Buttons.portfolioDelete) => {
      userDataStorage.deletePortfolio(userId, portfolio.name).onComplete {
        case Success(_) =>
          printPortfolios()
      }
    }
    case IncomingMessage(Buttons.notifications) => {
      userDataStorage.getUserPortfolioNotification(userId, portfolio.name) onComplete {
        case Success(notification) =>
          sendInlineMessageToUser(
            GeneralTexts.PORTFOLIO_DAILY_NOTIFICATION(portfolio.name, notification),
            GeneralMarkups.generatePortfolioNotificationOptions(userId, portfolio)
          )
          context become waitForPortfolioNotificationTime(portfolio)
        case _ =>
          sendMessageToUser(GeneralTexts.ERROR)
          returnToStartMenu()
      }

      context become waitForPortfolioName
    }
    case IncomingMessage(Buttons.triggers) => {
      printPortfolioTriggers(userId, portfolio)
      context become waitForPortfolioTrigger(portfolio)
    }
  }

  def waitForPortfolioName: Receive = common orElse {
    case IncomingMessage(portfolioName(name)) => {
      sendMessageToUser(GeneralTexts.INPUT_PORTFOLIO_CURRENCY(name))
      context become waitForPortfolioCurrency(name)
    }
    case _ => sendMessageToUser(GeneralTexts.INPUT_PORTFOLIO_NAME_INVALID)
  }

  def waitForPortfolioCurrency(name: String): Receive = common orElse {
    case IncomingMessage(currencyName(currency)) => {
      userDataStorage.addPortfolio(Portfolio(userId, name, Currency.define(currency), Map.empty)).onComplete {
        case Success(_) =>
          sendMessageToUser(GeneralTexts.INPUT_PORTFOLIO_CREATED(name, currency))
          printPortfolios()
        case _ =>
          sendMessageToUser(GeneralTexts.PORTFOLIO_CREATE_ERROR)
          returnToStartMenu()
      }
    }
    case _ => sendMessageToUser(GeneralTexts.INPUT_PORTFOLIO_CURRENCY_INVALID)
  }

  def waitForPortfolioStock(portfolio: Portfolio): Receive = portfolioMenu(portfolio) orElse {
    case IncomingMessage(stockName(name)) => {
      // Try to get the ticker

      if (!cache.contains(name)) {
        sendMessageToUser(GeneralTexts.PORTFOLIO_STOCK_ADD_QUERY)
      }

      cache.getStockInfo(name).onComplete {
        case Success(_) =>
          sendMessageToUser(GeneralTexts.PORTFOLIO_STOCK_ADD_AMOUNT(name, portfolio.name))
          context become waitForPortfolioStockAmount(portfolio, name)
        case Failure(exception) =>
          sendMessageToUser(GeneralTexts.printStockException(name))
          logger.warning(s"$exception", exception)
          context become waitForPortfolioStock(portfolio)
        case _ =>
          logger.warning(s"Unknown getStockInfo exception")
          returnToStartMenu()
      }

      context become Actor.emptyBehavior
    }
    case IncomingCallback(CallbackTypes.portfolioDeleteStock, x) => {
      userDataStorage.deleteStockFromPortfolio(userId, portfolio.name, x.message).onComplete {
        case Success(_) =>
          sendMessageToUser(GeneralTexts.PORTFOLIO_STOCK_DELETE_SUCCESS(x.message, portfolio.name))
          printPortfolio(userId, portfolio.name)
        case _ =>
          sendMessageToUser(GeneralTexts.PORTFOLIO_STOCK_DELETE_FAIL(x.message, portfolio.name), GeneralMarkups.viewPortfolioMarkup)
          printPortfolio(userId, portfolio.name)
      }
    }
  }

  def waitForPortfolioStockAmount(portfolio: Portfolio, stockName: String): Receive = portfolioMenu(portfolio) orElse {
    case IncomingMessage(floatAmount(amount)) =>
      userDataStorage.addStockToPortfolio(userId, portfolio.name, stockName, amount.toDouble) onComplete {
        case Success(_) =>
          printPortfolio(userId, portfolio.name)
        case _ =>
          sendMessageToUser(GeneralTexts.PORTFOLIO_STOCK_ADD_ERROR)
          printPortfolios()
      }
  }

  def waitForStock: Receive = common orElse {

    case IncomingMessage(stockName(name)) => {
      logger.info(s"Got message : $name")
      cache.getStockInfo(name).onComplete {
        case Success(price) =>
          sendMessageToUser(GeneralTexts.printStockPrice(name, price.price.toDouble))
          context become waitForStock
        case Failure(exception) =>
          sendMessageToUser(GeneralTexts.printStockException(name))
          logger.warning(s"$exception", exception)
          context become waitForStock
      }
      context become Actor.emptyBehavior
    }

  }

  def waitForNewNotification: Receive = {
    case IncomingMessage(notificationRegex(stock, bound, price)) =>
      val notificationType = bound match {
        case ">" => RaiseNotification
        case "<" => FallNotification
      }
      val notification = StockTriggerNotification(userId, stock, price.toDouble, notificationType)
      val currentPriceFuture = for {
        _ <- userDataStorage.addTriggerNotification(notification)
        price <- cache.getStockInfo(stock)
      } yield price
      currentPriceFuture.onComplete {
        case Success(stockPrice) =>
          sendMessageToUser(s"Notification added, you will be notified when $stock " +
            s"price will be ${if (notificationType == RaiseNotification) "upper" else "lower"} " +
            s"than $price (current price is $stockPrice)")
          returnToStartMenu()
        case Failure(exception) =>
          sendMessageToUser(s"Can't add notification: $exception")
          returnToStartMenu()
      }
      context become Actor.emptyBehavior
  }

  def waitForNotificationToDelete: Receive = {
    case IncomingMessage(notificationRegex(stock, bound, price)) =>
      val notificationType = bound match {
        case ">" => RaiseNotification
        case "<" => FallNotification
      }
      val notification = StockTriggerNotification(userId, stock, price.toDouble, notificationType)
      userDataStorage.deleteTriggerNotification(notification).onComplete {
        case Success(_) =>
          sendMessageToUser(s"Notification ${GeneralMarkups.notificationToString(notification)} has been deleted")
          returnToStartMenu()
        case Failure(exception) =>
          sendMessageToUser(s"Can't delete notification: $exception")
          returnToStartMenu()
      }
      context become Actor.emptyBehavior
  }

  def notificationsMenu: Receive = common orElse {
    //    case IncomingMessage(Buttons.backToMain) =>
    //      returnToStartMenu()
    case IncomingMessage(Buttons.notificationGet) =>
      userDataStorage.getUsersTriggerNotifications(userId).onComplete {
        case Success(result) =>
          val notifications = result.map(GeneralMarkups.notificationToString)
          sendMessageToUser("Active notifications: \n" + notifications.mkString("\n"))
          returnToStartMenu()
        case Failure(_) =>
          sendMessageToUser("Can't load your active notifications")
          returnToStartMenu()
      }
      context become Actor.emptyBehavior
    case IncomingMessage(Buttons.notificationAdd) =>
      sendMessageToUser("Enter notification(for example \"MSFT > 95\"): ")
      context become waitForNewNotification
    case IncomingMessage(Buttons.notificationDel) =>
      userDataStorage.getUsersTriggerNotifications(userId).onComplete {
        case Success(result) =>
          sendMessageToUser("Choose notification to delete", GeneralMarkups.notificationsMarkup(result))
          context become waitForNotificationToDelete
        case Failure(_) =>
          sendMessageToUser("Can't load your active notifications")
          returnToStartMenu()
      }
      context become Actor.emptyBehavior
  }*/
}

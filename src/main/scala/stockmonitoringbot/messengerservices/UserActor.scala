package stockmonitoringbot.messengerservices

import java.time.LocalTime
import java.time.format.DateTimeFormatter

import akka.actor.{Actor, Props}
import akka.event.Logging
import info.mukel.telegrambot4s.methods.SendMessage
import info.mukel.telegrambot4s.models.{InlineKeyboardMarkup, ReplyKeyboardMarkup}
import stockmonitoringbot.datastorage._
import stockmonitoringbot.datastorage.models._
import stockmonitoringbot.messengerservices.MessageSenderComponent.MessageSender
import stockmonitoringbot.messengerservices.UserActor._
import stockmonitoringbot.messengerservices.markups.{Buttons, GeneralMarkups, GeneralTexts}
import stockmonitoringbot.notificationhandlers.{DailyNotificationHandler, getPortfolioCurrentPrice}
import stockmonitoringbot.stocksandratescache.PriceCache

import scala.util.matching.Regex
import scala.util.{Failure, Success}

/**
  * Created by amir.
  */
class UserActor(userId: Long,
                messageSender: MessageSender,
                userDataStorage: UserDataStorage,
                dailyNotification: DailyNotificationHandler,
                cache: PriceCache) extends Actor {

  import context.dispatcher

  val logger = Logging(context.system, this)

  private def sendMessageToUser(message: String, markup: Option[ReplyKeyboardMarkup] = None): Unit =
    messageSender(SendMessage(userId, message, replyMarkup = markup))

  private def sendInlineMessageToUser(message: String, markup: Option[InlineKeyboardMarkup]): Unit =
    messageSender(SendMessage(userId, message, replyMarkup = markup))

  override def preStart(): Unit = {
    sendMessageToUser(GeneralTexts.INTRO_MESSAGE, GeneralMarkups.startMenuMarkup)
  }

  def returnToStartMenu(): Unit = {
    sendMessageToUser(GeneralTexts.MAIN_MENU_GREETING, GeneralMarkups.startMenuMarkup)
    context become startMenu
  }

  def printPortfolios(): Unit = {
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
      case _ =>
        returnToStartMenu()
    }
  }

  def printPortfolio(userId: Long, portfolioName: String): Unit = {
    userDataStorage.getPortfolio(userId, portfolioName) map {
      portfolio =>
        val portfolioGreeting = GeneralTexts.PORTFOLIO_SHOW(portfolio) +
          (
            if (portfolio.stocks.isEmpty)
              ""
            else
              GeneralTexts.PORTFOLIO_SHOW_STOCK +
                portfolio.stocks.map {
                  case (k, v) =>
                    s"$k ($v)"
                }.mkString("\n")
            )
        // TODO: Calculate sum
        sendMessageToUser(portfolioGreeting, GeneralMarkups.viewPortfolioMarkup)
        context become portfolioMenu(portfolio)
    }
  }

  def printPortfolioTriggers(userId: Long, portfolio: Portfolio): Unit = {
    getPortfolioCurrentPrice(portfolio, cache) onComplete {
      case Success(price) =>
        sendMessageToUser(GeneralTexts.PORTFOLIO_TRIGGERS(portfolio.name, price))
      case _ =>

    }
  }

  def clearPortfolioNotification(userId: Long, portfolio: Portfolio): Unit = {
    userDataStorage.getUserPortfolioNotification(userId, portfolio.name).onComplete {
      case Success(Some(x)) => {
        dailyNotification.deleteDailyNotification(x)
        userDataStorage.deleteDailyNotification(x)
      }
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
      sendMessageToUser(GeneralTexts.PORTFOLIO_DAILY_NOTIFICATION_SET(time))
    }
    catch {
      case e: Exception => sendMessageToUser(GeneralTexts.TIME_ERROR)
    }
  }

  override def receive: Receive = startMenu

  // Telegram callback handlers
  def tgCallback: Receive = {
    case IncomingCallback(CallbackTypes.portfolio, data) =>
      printPortfolio(userId, data.message)
  }

  // General menu items
  def common: Receive = tgCallback orElse {

    case IncomingMessage(Buttons.backToMain) =>
      returnToStartMenu()

    case IncomingMessage(Buttons.stock) =>
      sendMessageToUser(GeneralTexts.STOCK_INTRO_MESSAGE, GeneralMarkups.stockMarkup)
      context become waitForStock

    case IncomingMessage(Buttons.portfolio) =>
      printPortfolios()

    case IncomingMessage(Buttons.currency) | IncomingMessage(Buttons.info) =>
      sendMessageToUser(GeneralTexts.UNIMPLEMENTED)
  }

  def startMenu: Receive = common orElse {
    case IncomingMessage(Buttons.notifications) =>
      sendMessageToUser("Notification menu", GeneralMarkups.notificationsMenuMarkup)
      context become notificationsMenu
    case IncomingMessage(Buttons.triggers) =>
      sendMessageToUser("Triggers menu", GeneralMarkups.notificationsMenuMarkup)
      context become notificationsMenu
  }

  def waitForPortfolioTrigger(portfolio: Portfolio): Receive = common orElse {
    case IncomingMessage(floatAmount(bound)) =>
      logger.info(bound)
  }

  def waitForPortfolioNotificationTime(portfolio: Portfolio): Receive = common orElse {

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

  def waitForPortfolioStock(portfolio: Portfolio): Receive = common orElse {
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

  def waitForPortfolioStockAmount(portfolio: Portfolio, stockName: String): Receive = common orElse {
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
  }

}

case class IncomingCallbackMessage(userId: String, message: String)
object CallbackTypes {
  val portfolio = "PRT"
  val portfolioSetNotification = "PRN"
  val portfolioDeleteStock = "PRD"
}

object UserActor {
  def props(id: Long, messageSender: MessageSender, userDataStorage: UserDataStorage, dailyNotificationHandler: DailyNotificationHandler, cache: PriceCache): Props =
    Props(new UserActor(id, messageSender, userDataStorage, dailyNotificationHandler, cache))

  case class IncomingMessage(message: String)
  case class IncomingCallback(handler: String, message: IncomingCallbackMessage)

  val stockName: Regex = "/?([A-Z]+)".r
  val notificationRegex: Regex = "([A-Z]+) ([<>]) ([^ ]+)".r
  val portfolioName: Regex = "([a-zA-Z0-9_\\-\\ ]{3,64})".r
  val currencyName: Regex = "(USD|EUR|RUB)".r
  val floatAmount: Regex = "([0-9]+[.]?[0-9]*)".r

}
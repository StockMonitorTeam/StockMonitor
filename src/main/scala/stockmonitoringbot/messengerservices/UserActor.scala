package stockmonitoringbot.messengerservices

import akka.actor.{Actor, Props}
import akka.event.Logging
import info.mukel.telegrambot4s.methods.SendMessage
import info.mukel.telegrambot4s.models.{InlineKeyboardMarkup, ReplyKeyboardMarkup}
import stockmonitoringbot.datastorage._
import stockmonitoringbot.datastorage.models._
import stockmonitoringbot.messengerservices.UserActor._
import stockmonitoringbot.messengerservices.markups.{Buttons, GeneralMarkups, GeneralTexts}
import stockmonitoringbot.stocksandratescache.StocksAndExchangeRatesCache

import scala.util.matching.Regex
import scala.util.{Failure, Success}

/**
  * Created by amir.
  */
class UserActor(userId: Long,
                telegramService: MessageSender,
                userDataStorage: UserDataStorage,
                cache: StocksAndExchangeRatesCache) extends Actor {

  import context.dispatcher

  val logger = Logging(context.system, this)

  private def sendMessageToUser(message: String, markup: Option[ReplyKeyboardMarkup] = None): Unit =
    telegramService.send(SendMessage(userId, message, replyMarkup = markup))

  private def sendInlineMessageToUser(message: String, markup: Option[InlineKeyboardMarkup]): Unit =
    telegramService.send(SendMessage(userId, message, replyMarkup = markup))

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
        context become waitForPortfolioStock(portfolio)
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

    case IncomingMessage(Buttons.currency) | IncomingMessage(Buttons.triggers) | IncomingMessage(Buttons.info) =>
      sendMessageToUser(GeneralTexts.UNIMPLEMENTED)
  }

  def startMenu: Receive = common orElse {
    case IncomingMessage(Buttons.notifications) =>
      sendMessageToUser("Notification menu", GeneralMarkups.notificationsMenuMarkup)
      context become notificationsMenu
  }

  def waitForPortfolio: Receive = common orElse {
    case IncomingMessage(Buttons.portfolioCreate) => {
      sendMessageToUser(GeneralTexts.INPUT_PORTFOLIO_NAME)
      context become waitForPortfolioName
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
    case IncomingMessage(Buttons.portfolioStockAdd) =>
      sendMessageToUser(GeneralTexts.PORTFOLIO_STOCK_ADD, GeneralMarkups.viewPortfolioMarkup)
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
          logger.warning(s"$exception")
          context become waitForPortfolioStock(portfolio)
        case _ =>
          logger.warning(s"Unknown getStockInfo exception")
          returnToStartMenu()
      }

      context become Actor.emptyBehavior
    }
  }

  def waitForPortfolioStockAmount(portfolio: Portfolio, stockName: String): Receive = common orElse {
    case IncomingMessage(floatAmount(amount)) =>
      userDataStorage.addStockToPortfolio(userId, portfolio.name, stockName, amount.toDouble) onComplete {
        case Success(_) =>
          printPortfolio(userId, portfolio.name)
          context become waitForPortfolio
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
          logger.warning(s"$exception")
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
}

object UserActor {
  def props(id: Long, telegramService: MessageSender, userDataStorage: UserDataStorage, cache: StocksAndExchangeRatesCache): Props =
    Props(new UserActor(id, telegramService, userDataStorage, cache))

  case class IncomingMessage(message: String)
  case class IncomingCallback(handler: String, message: IncomingCallbackMessage)

  val stockName: Regex = "/?([A-Z]+)".r
  val notificationRegex: Regex = "([A-Z]+) ([<>]) ([^ ]+)".r
  val portfolioName: Regex = "([a-zA-Z0-9_\\-\\ ]{3,64})".r
  val currencyName: Regex = "(USD|EUR|RUB)".r
  val floatAmount: Regex = "([0-9]+[.]?[0-9]*)".r

}
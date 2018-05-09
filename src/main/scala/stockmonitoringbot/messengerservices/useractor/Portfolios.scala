package stockmonitoringbot.messengerservices.useractor

import java.time.LocalTime
import java.time.format.DateTimeFormatter

import akka.actor.Actor.Receive
import stockmonitoringbot.datastorage.models._
import stockmonitoringbot.messengerservices.markups.{Buttons, GeneralMarkups, GeneralTexts}
import stockmonitoringbot.messengerservices.useractor.UserActor.{CallbackTypes, IncomingCallback, IncomingMessage, SetBehavior}
import stockmonitoringbot.notificationhandlers.{getPortfolioCurrentPrice, getPortfolioStocksPrice}

import scala.util.matching.Regex
import scala.util.{Failure, Success}

/**
  * Created by amir.
  */
trait Portfolios {
  this: MainStuff =>

  val portfolioName: Regex = "([a-zA-Z0-9_\\-\\ ]{3,64})".r
  val currencyName: Regex = "(USD|EUR|RUB)".r

  def becomePortfolioMainMenu(): Unit = {
    userDataStorage.getUserPortfolios(userId).onComplete {
      case Success(results) => results match {
        case Seq() =>
          sendMessageToUser(GeneralTexts.NO_PORTFOLIO_GREETING, GeneralMarkups.portfolioMarkup)
          self ! SetBehavior(waitForPortfolio)
        case portfolios =>
          sendMessageToUser(GeneralTexts.PORTFOLIO_HELLO, GeneralMarkups.portfolioMarkup)
          sendInlineMessageToUser(GeneralTexts.PORTFOLIO_LIST, GeneralMarkups.generatePortfolioList(userId, portfolios))
          self ! SetBehavior(waitForPortfolio)
      }
      case Failure(e) =>
        logger.error("Can't get portfolios", e)
        sendMessageToUser(GeneralTexts.ERROR)
    }
  }

  def waitForPortfolio: Receive = common orElse {
    case IncomingMessage(Buttons.portfolioCreate) =>
      sendMessageToUser(GeneralTexts.INPUT_PORTFOLIO_NAME, GeneralMarkups.onlyMainMenu)
      context become waitForPortfolioName
  }

  // Telegram callback handlers
  def tgCallback: Receive = {
    case IncomingCallback(CallbackTypes.portfolio, data) =>
      context become waitForNewBehavior()
      printPortfolio(data.message)
  }

  // General menu items
  def common: Receive = tgCallback orElse {
    case IncomingMessage(Buttons.backToMain) =>
      becomeMainMenu()
    case IncomingMessage(Buttons.portfolio) =>
      context become waitForNewBehavior()
      printPortfolios()
  }

  def printPortfolios(): Unit = {
    userDataStorage.getUserPortfolios(userId).onComplete {
      case Success(results) => results match {
        case Seq() =>
          sendMessageToUser(GeneralTexts.NO_PORTFOLIO_GREETING, GeneralMarkups.portfolioMarkup)
          self ! SetBehavior(waitForPortfolio)
        case portfolios =>
          sendMessageToUser(GeneralTexts.PORTFOLIO_HELLO, GeneralMarkups.portfolioMarkup)
          sendInlineMessageToUser(GeneralTexts.PORTFOLIO_LIST, GeneralMarkups.generatePortfolioList(userId, portfolios))
          self ! SetBehavior(waitForPortfolio)
      }
      case _ =>
        becomePortfolioMainMenu()
    }
  }

  def printPortfolio(portfolioName: String): Unit = {
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
      self ! SetBehavior(portfolioMenu(portfolio))
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
      case _: Exception => sendMessageToUser(GeneralTexts.TIME_ERROR)
    }
  }

  def createNewPortfolio(name: String, currency: String): Unit = {
    userDataStorage.addPortfolio(Portfolio(userId, name, Currency.define(currency), Map.empty)).onComplete {
      case Success(_) =>
        sendMessageToUser(GeneralTexts.INPUT_PORTFOLIO_CREATED(name, currency))
        printPortfolios()
      case _ =>
        sendMessageToUser(GeneralTexts.PORTFOLIO_CREATE_ERROR)
        becomePortfolioMainMenu()
    }
  }

  def waitForPortfolioTrigger(portfolio: Portfolio): Receive = {
    case IncomingMessage(Buttons.triggerAdd) =>
      addTriggerNotification(PortfolioAsset(portfolio.name), {
        printPortfolio(portfolio.name)
      })
    case IncomingMessage(Buttons.triggerRemove) =>
      userDataStorage.getUserTriggerNotification(userId, PortfolioAsset(portfolio.name)) onComplete {
        case Success(Nil) =>
          sendMessageToUser(GeneralTexts.PORTFOLIO_TRIGGER_EMPTY)
        case Success(x) =>
          sendInlineMessageToUser(GeneralTexts.PORTFOLIO_TRIGGER_REMOVE, GeneralMarkups.generatePortfolioTriggersDelete(userId, x))
        case _ =>
          sendMessageToUser(GeneralTexts.ERROR)
      }
    case IncomingMessage(Buttons.back) =>
      printPortfolio(portfolio.name)
      context become waitForNewBehavior()
    case IncomingCallback(CallbackTypes.portfolioDeleteTrigger, message) =>
      message.message.split(" - ", 2) match {
        case Array(notificationType, boundPrice) =>
          val notification = PortfolioTriggerNotification(userId, portfolio.name, BigDecimal(boundPrice), TriggerNotificationType.define(notificationType))
          userDataStorage.deleteTriggerNotification(notification)
          sendMessageToUser(GeneralTexts.PORTFOLIO_TRIGGER_REMOVED(message.message))
        case _ =>
          sendMessageToUser(GeneralTexts.ERROR)
      }
  }

  def portfolioMenu(portfolio: Portfolio): Receive = common orElse {
    case IncomingMessage(Buttons.portfolioStockAdd) =>
      sendMessageToUser(GeneralTexts.PORTFOLIO_STOCK_ADD(portfolio.name), GeneralMarkups.viewPortfolioMarkup)
      context become waitForPortfolioStock(portfolio)
    case IncomingMessage(Buttons.portfolioStockDelete) =>
      if (portfolio.stocks.isEmpty) {
        sendMessageToUser(GeneralTexts.PORTFOLIO_STOCK_EMPTY(portfolio.name))
      } else {
        sendInlineMessageToUser(GeneralTexts.PORTFOLIO_STOCK_DELETE(portfolio.name), GeneralMarkups.generatePortfolioStockDelete(userId, portfolio))
        context become waitForPortfolioStock(portfolio)
      }
    case IncomingMessage(Buttons.portfolioDelete) =>
      userDataStorage.deletePortfolio(userId, portfolio.name).onComplete {
        case Success(_) =>
          printPortfolios()
      }
      context become waitForPortfolioName
    case IncomingMessage(Buttons.notifications) =>
      userDataStorage.getUserNotification(userId, PortfolioAsset(portfolio.name)) onComplete {
        case Success(_) =>
          addDailyNotification(PortfolioAsset(portfolio.name), {
            printPortfolio(portfolio.name)
          })
        case _ =>
          sendMessageToUser(GeneralTexts.ERROR)
          becomePortfolioMainMenu()
      }
      context become waitForPortfolioName
    case IncomingMessage(Buttons.triggers) =>
      printPortfolioTriggers(userId, portfolio)
      context become waitForPortfolioTrigger(portfolio)
  }

  def waitForPortfolioName: Receive = common orElse {
    case IncomingMessage(portfolioName(name)) =>
      userDataStorage.getPortfolio(userId, name) onComplete {
        case Success(_) =>
          sendMessageToUser(GeneralTexts.INPUT_PORTFOLIO_NAME_EXISTS)
          self ! SetBehavior(waitForPortfolioName)
        case _ =>
          sendMessageToUser(GeneralTexts.INPUT_PORTFOLIO_CURRENCY(name))
          sendInlineMessageToUser(GeneralTexts.INPUT_PORTFOLIO_CURRENCY_LIST, GeneralMarkups.portfolioCurrencySwitch(userId))
          self ! SetBehavior(waitForPortfolioCurrency(name))
      }
      context become waitForNewBehavior()
    case _ => sendMessageToUser(GeneralTexts.INPUT_PORTFOLIO_NAME_INVALID)
  }

  def waitForPortfolioCurrency(name: String): Receive = {
    case IncomingMessage(currencyName(currency)) =>
      createNewPortfolio(name, currency)
      context become waitForNewBehavior()
    case IncomingCallback(CallbackTypes.choseCurrency, data) =>
      createNewPortfolio(name, data.message)
      context become waitForNewBehavior()
    case IncomingMessage(Buttons.backToMain) => becomeMainMenu()
    case _ => sendMessageToUser(GeneralTexts.INPUT_PORTFOLIO_CURRENCY_INVALID)
  }

  def waitForPortfolioStock(portfolio: Portfolio): Receive = portfolioMenu(portfolio) orElse {
    case IncomingMessage(stockName(name)) =>
      // Try to get the ticker

      if (!cache.contains(name)) {
        sendMessageToUser(GeneralTexts.PORTFOLIO_STOCK_ADD_QUERY)
      }

      cache.getStockInfo(name).onComplete {
        case Success(_) =>
          sendMessageToUser(GeneralTexts.PORTFOLIO_STOCK_ADD_AMOUNT(name, portfolio.name))
          self ! SetBehavior(waitForPortfolioStockAmount(portfolio, name))
        case Failure(exception) =>
          sendMessageToUser(GeneralTexts.printStockException(name))
          logger.warn(s"$exception", exception)
          self ! SetBehavior(waitForPortfolioStock(portfolio))
        case _ =>
          logger.warn(s"Unknown getStockInfo exception")
          becomePortfolioMainMenu()
      }
      context become waitForNewBehavior()
    case IncomingCallback(CallbackTypes.portfolioDeleteStock, x) =>
      userDataStorage.deleteStockFromPortfolio(userId, portfolio.name, x.message).onComplete {
        case Success(_) =>
          sendMessageToUser(GeneralTexts.PORTFOLIO_STOCK_DELETE_SUCCESS(x.message, portfolio.name))
          printPortfolio(portfolio.name)
        case _ =>
          sendMessageToUser(GeneralTexts.PORTFOLIO_STOCK_DELETE_FAIL(x.message, portfolio.name), GeneralMarkups.viewPortfolioMarkup)
          printPortfolio(portfolio.name)
      }
      context become waitForNewBehavior()
  }

  def waitForPortfolioStockAmount(portfolio: Portfolio, stockName: String): Receive = portfolioMenu(portfolio) orElse {
    case IncomingMessage(floatAmount(amount)) =>
      userDataStorage.addStockToPortfolio(userId, portfolio.name, stockName, amount.toDouble) onComplete {
        case Success(_) =>
          printPortfolio(portfolio.name)
        case _ =>
          sendMessageToUser(GeneralTexts.PORTFOLIO_STOCK_ADD_ERROR)
          printPortfolios()
      }
      context become waitForNewBehavior()
  }

}

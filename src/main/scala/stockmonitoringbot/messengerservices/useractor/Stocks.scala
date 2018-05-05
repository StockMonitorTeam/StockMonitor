package stockmonitoringbot.messengerservices.useractor

import akka.actor.Actor.Receive
import stockmonitoringbot.datastorage.models.StockAsset
import stockmonitoringbot.messengerservices.markups.{Buttons, GeneralMarkups, GeneralTexts}
import stockmonitoringbot.messengerservices.useractor.UserActor.{IncomingMessage, SetBehavior}
import stockmonitoringbot.stockpriceservices.StockInfo

import scala.util.matching.Regex
import scala.util.{Failure, Success}

/**
  * Created by amir.
  */
trait Stocks {
  this: MainStuff =>

  def becomeStockMainMenu(): Unit = {
    sendMessageToUser(GeneralTexts.STOCK_INTRO_MESSAGE, GeneralMarkups.stocksMenuMarkup)
    context become waitForStock
  }

  //#2
  def waitForStock: Receive = {
    case IncomingMessage(stockName(name)) =>
      logger.info(s"Got message : $name")
      cache.getStockInfo(name).onComplete {
        case Success(stock) =>
          goToStockMenu(stock)
        case Failure(exception) =>
          sendMessageToUser(GeneralTexts.printStockException(name))
          logger.warn(s"$exception", exception)
          self ! SetBehavior(waitForStock)
      }
      context become waitForNewBehavior()
    case IncomingMessage(Buttons.backToMain) =>
      becomeMainMenu()
  }

  def goToStockMenu(stock: StockInfo): Unit = {
    val dailyNotFut = userDataStorage.getUserNotification(userId, StockAsset(stock.name))
    val triggerNotFut = userDataStorage.getUserTriggerNotification(userId, StockAsset(stock.name))
    for {dailyNot <- dailyNotFut
         triggerNot <- triggerNotFut
    } {
      sendMessageToUser(GeneralTexts.printStockPrice(stock.name, stock.price.toDouble, triggerNot, dailyNot),
        GeneralMarkups.oneStockMenuMarkup)
      self ! SetBehavior(stockMenu(stock))
    }
  }

  //#6
  def stockMenu(stock: StockInfo): Receive = {
    case IncomingMessage(Buttons.notificationAdd) => addDailyNotification(StockAsset(stock.name), goToStockMenu(stock))
    case IncomingMessage(Buttons.triggerAdd) => addTriggerNotification(StockAsset(stock.name), goToStockMenu(stock))
    case IncomingMessage(Buttons.stock) => becomeStockMainMenu()
    case IncomingMessage(Buttons.backToMain) => becomeMainMenu()
  }

  val stockName: Regex = "/?([A-Z]+)".r
}

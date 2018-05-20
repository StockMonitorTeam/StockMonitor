package stockmonitoringbot.messengerservices.useractor

import akka.actor.Actor.Receive
import stockmonitoringbot.datastorage.models.StockAsset
import stockmonitoringbot.messengerservices.markups.{Buttons, GeneralMarkups, GeneralTexts}
import stockmonitoringbot.messengerservices.useractor.UserActor.{IncomingMessage, SetBehavior}
import stockmonitoringbot.stockpriceservices.models.StockInfo

import scala.util.{Failure, Success}

/**
  * Created by amir.
  */
trait Stocks {
  this: MainStuff =>

  def becomeStockMainMenu(): Unit = {
    sendMessageToUser(GeneralTexts.STOCK_INTRO_MESSAGE, GeneralMarkups.onlyMainMenu)
    context become waitForStock
  }

  //#2
  def waitForStock: Receive = {
    case IncomingMessage(stockName(name)) =>
      logger.info(s"Got message : $name")
      userActorService.getStockInfo(name).onComplete {
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
    case IncomingMessage(_) =>
      sendMessageToUser(GeneralTexts.WRONG_STOCK_NAME)
  }

  def goToStockMenu(stock: StockInfo): Unit = {
    val dailyNotFut = userActorService.getUserNotificationOnAsset(userId, StockAsset(stock.name))
    val triggerNotFut = userActorService.getUserTriggerNotificationOnAsset(userId, StockAsset(stock.name))
    val userFut = userActorService.getUser(userId)
    for {dailyNot <- dailyNotFut
         triggerNot <- triggerNotFut
         user <- userFut
    } {
      sendMessageToUser(GeneralTexts.printStockPrice(stock.name, stock.price.toDouble, triggerNot, dailyNot, user.get),
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

}

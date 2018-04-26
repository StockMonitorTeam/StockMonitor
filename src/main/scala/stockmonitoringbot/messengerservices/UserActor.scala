package stockmonitoringbot.messengerservices

import akka.actor.{Actor, Props}
import info.mukel.telegrambot4s.methods.SendMessage
import info.mukel.telegrambot4s.models.{KeyboardButton, ReplyKeyboardMarkup}
import stockmonitoringbot.datastorage.{DataStorage, FallNotification, Notification, RaiseNotification}
import stockmonitoringbot.messengerservices.UserActor._

import scala.util.matching.Regex
import scala.util.{Failure, Success}
import stockmonitoringbot.messengerservices.markups.{Buttons, GeneralMarkups, GeneralTexts}

import akka.event.Logging

/**
  * Created by amir.
  */
class UserActor(userId: Long, telegramService: MessageSender, notificationService: DataStorage) extends Actor {

  import context.dispatcher

  val logger = Logging(context.system, this)

  private def sendMessageToUser(message: String, markup: Option[ReplyKeyboardMarkup] = None): Unit =
    telegramService.send(SendMessage(userId, message, replyMarkup = markup))

  override def preStart(): Unit = {
    sendMessageToUser(GeneralTexts.INTRO_MESSAGE, GeneralMarkups.startMenuMarkup)
  }

  override def receive: Receive = startMenu

  def returnToStartMenu(): Unit = {
    sendMessageToUser(GeneralTexts.MAIN_MENU_GREETING, GeneralMarkups.startMenuMarkup)
    context become startMenu
  }

  def startMenu: Receive = {
    case IncomingMessage(Buttons.stock) =>
      sendMessageToUser(GeneralTexts.STOCK_INTRO_MESSAGE, GeneralMarkups.stockMarkup)
      context become waitForStock

    case IncomingMessage(Buttons.currency) | IncomingMessage(Buttons.collection)
    | IncomingMessage(Buttons.triggers) | IncomingMessage(Buttons.info) =>
      sendMessageToUser(GeneralTexts.UNIMPLEMENTED)

    case IncomingMessage(Buttons.notifications) =>
      sendMessageToUser("Notification menu", GeneralMarkups.notificationsMenuMarkup)
      context become notificationsMenu
  }

  def waitForStock: Receive = {
    case IncomingMessage(Buttons.backToMain) =>
      returnToStartMenu()

    case IncomingMessage(stockName(name)) => {
      logger.info(s"Got message : $name")
      notificationService.getPrice(name).onComplete {
        case Success(price) =>
          sendMessageToUser(GeneralTexts.printStockPrice(name, price))
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
      val notification = Notification(stock, price.toDouble, notificationType, userId)
      val currentPriceFuture = for {
        _ <- notificationService.addNotification(notification)
        price <- notificationService.getPrice(stock)
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
      val notification = Notification(stock, price.toDouble, notificationType, userId)
      notificationService.deleteNotification(notification).onComplete {
        case Success(_) =>
          sendMessageToUser(s"Notification ${GeneralMarkups.notificationToString(notification)} has been deleted")
          returnToStartMenu()
        case Failure(exception) =>
          sendMessageToUser(s"Can't delete notification: $exception")
          returnToStartMenu()
      }
      context become Actor.emptyBehavior
  }

  def notificationsMenu: Receive = {
    case IncomingMessage(Buttons.notificationGet) =>
      notificationService.getNotifications(userId).onComplete {
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
      notificationService.getNotifications(userId).onComplete {
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

object UserActor {
  def props(id: Long, telegramService: MessageSender, notificationService: DataStorage): Props =
    Props(new UserActor(id, telegramService, notificationService))

  case class IncomingMessage(message: String)

  val stockName: Regex = "/?([A-Z]+)".r
  val notificationRegex: Regex = "([A-Z]+) ([<>]) ([^ ]+)".r

}
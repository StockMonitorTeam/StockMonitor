package stockmonitoringbot.messengerservices

import akka.actor.{Actor, ActorLogging, Props}
import info.mukel.telegrambot4s.methods.SendMessage
import info.mukel.telegrambot4s.models.{KeyboardButton, ReplyKeyboardMarkup}
import stockmonitoringbot.datastorage.{DataStorage, FallNotification, Notification, RaiseNotification}
import stockmonitoringbot.messengerservices.UserActor._

import scala.util.matching.Regex
import scala.util.{Failure, Success}

/**
  * Created by amir.
  */
class UserActor(userId: Long, telegramService: TelegramService, notificationService: DataStorage) extends Actor with ActorLogging {

  import context.dispatcher

  private def sendMessageToUser(message: String, markup: Option[ReplyKeyboardMarkup] = None): Unit = {
    telegramService.send(SendMessage(userId, message, replyMarkup = markup))
  }

  override def preStart(): Unit = {
    sendMessageToUser("Hello, choose action:", startMenuMarkup)
  }

  override def receive: Receive = startMenu

  def returnToStartMenu(): Unit = {
    sendMessageToUser("Choose Action: ", startMenuMarkup)
    context become startMenu
  }

  def startMenu: Receive = {
    case IncomingMessage(`getStockPrice`) =>
      sendMessageToUser("Enter stock name")
      context become waitForStock
    case IncomingMessage(`notifications`) =>
      sendMessageToUser("Notification menu", notificationsMenuMarkup)
      context become notificationsMenu
  }

  def waitForStock: Receive = {
    case IncomingMessage(stockName(name)) =>
      notificationService.getPrice(name).onComplete {
        case Success(price) =>
          sendMessageToUser(s"$name current price is $price")
          returnToStartMenu()
        case Failure(exception) =>
          sendMessageToUser(s"$name is not valid stock name $exception")
          returnToStartMenu()
      }
      context become Actor.emptyBehavior
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
          sendMessageToUser(s"Notification ${notificationToString(notification)} has been deleted")
          returnToStartMenu()
        case Failure(exception) =>
          sendMessageToUser(s"Can't delete notification: $exception")
          returnToStartMenu()
      }
      context become Actor.emptyBehavior
  }

  def notificationsMenu: Receive = {
    case IncomingMessage(`getActiveNotifications`) =>
      notificationService.getNotifications(userId).onComplete {
        case Success(result) =>
          val notifications = result.map(notificationToString)
          sendMessageToUser("Active notifications: \n" + notifications.mkString("\n"))
          returnToStartMenu()
        case Failure(_) =>
          sendMessageToUser("Can't load your active notifications")
          returnToStartMenu()
      }
      context become Actor.emptyBehavior
    case IncomingMessage(`addNotification`) =>
      sendMessageToUser("Enter notification(for example \"MSFT > 95\"): ")
      context become waitForNewNotification
    case IncomingMessage(`delNotification`) =>
      notificationService.getNotifications(userId).onComplete {
        case Success(result) =>
          sendMessageToUser("Choose notification to delete", notificationsMarkup(result))
          context become waitForNotificationToDelete
        case Failure(_) =>
          sendMessageToUser("Can't load your active notifications")
          returnToStartMenu()
      }
      context become Actor.emptyBehavior
  }

}

object UserActor {
  def props(id: Long, telegramService: TelegramService, notificationService: DataStorage): Props =
    Props(new UserActor(id, telegramService, notificationService))

  case class IncomingMessage(message: String)

  val getStockPrice = "Get stock price"
  val notifications = "Notifications"
  val getActiveNotifications = "Get active notifications"
  val addNotification = "Add notification"
  val delNotification = "Delete notification"

  val stockName: Regex = "([A-Z]+)".r
  val notificationRegex: Regex = "([A-Z]+) ([<>]) ([^ ]+)".r

  val startMenuMarkup: Option[ReplyKeyboardMarkup] = Some(ReplyKeyboardMarkup.singleColumn(
    Seq(KeyboardButton(getStockPrice), KeyboardButton(notifications)),
    resizeKeyboard = Some(true),
    oneTimeKeyboard = Some(true)))

  val notificationsMenuMarkup: Option[ReplyKeyboardMarkup] = Some(ReplyKeyboardMarkup.singleColumn(
    Seq(KeyboardButton(getActiveNotifications), KeyboardButton(addNotification), KeyboardButton(delNotification)),
    resizeKeyboard = Some(true),
    oneTimeKeyboard = Some(true)))

  def notificationsMarkup(notifications: Seq[Notification]): Option[ReplyKeyboardMarkup] = Some(ReplyKeyboardMarkup.singleColumn(
    notifications.map(notification => KeyboardButton(notificationToString(notification))),
    resizeKeyboard = Some(true),
    oneTimeKeyboard = Some(true)))

  def notificationToString(notification: Notification): String = {
    val notificationType = notification.notificationType match {
      case RaiseNotification => ">"
      case FallNotification => "<"
    }
    s"${notification.stock} $notificationType ${notification.price}"
  }

}
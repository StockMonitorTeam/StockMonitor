package stockmonitoringbot.messengerservices.useractor

import java.time.LocalTime
import java.time.format.{DateTimeFormatter, DateTimeParseException}

import akka.actor.Actor.Receive
import akka.actor.{ActorContext, ActorRef}
import com.typesafe.scalalogging.Logger
import info.mukel.telegrambot4s.methods.SendMessage
import info.mukel.telegrambot4s.models.{InlineKeyboardMarkup, ReplyKeyboardMarkup, ReplyKeyboardRemove}
import stockmonitoringbot.datastorage.UserDataStorage
import stockmonitoringbot.datastorage.models._
import stockmonitoringbot.messengerservices.MessageSenderComponent.MessageSender
import stockmonitoringbot.messengerservices.markups.{Buttons, GeneralMarkups, GeneralTexts}
import stockmonitoringbot.messengerservices.useractor.MainStuff._
import stockmonitoringbot.messengerservices.useractor.UserActor.{CallbackTypes, IncomingCallback, IncomingMessage, SetBehavior}
import stockmonitoringbot.notificationhandlers.DailyNotificationHandler
import stockmonitoringbot.stocksandratescache.PriceCache

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import scala.util.matching.Regex

/**
  * Created by amir.
  */
trait MainStuff {
  val userId: Long
  val messageSender: MessageSender
  val userDataStorage: UserDataStorage
  val dailyNotification: DailyNotificationHandler
  val cache: PriceCache
  val logger: Logger
  val self: ActorRef
  implicit val context: ActorContext
  implicit val ec: ExecutionContext

  def sendMessageToUser(message: String, markup: Option[ReplyKeyboardMarkup] = None): Unit =
    messageSender(SendMessage(userId, message, disableWebPagePreview = Some(true), replyMarkup = markup))

  def messageHideKeyboard(): Unit =
    messageSender(SendMessage(userId, "23", disableWebPagePreview = Some(true), replyMarkup = Some(ReplyKeyboardRemove())))

  def sendInlineMessageToUser(message: String, markup: Option[InlineKeyboardMarkup]): Unit =
    messageSender(SendMessage(userId, message, replyMarkup = markup))

  def becomeMainMenu(): Unit

  def waitForNewBehavior(msgToForward: Seq[Any] = Seq()): Receive = {
    case SetBehavior(behavior) =>
      context become behavior
      msgToForward.foreach(self ! _)
    case anyMsg =>
      context become waitForNewBehavior(msgToForward :+ anyMsg)
  }

  //NEW TRIGGER
  //callback should send SetBehavior message to self to take control back
  def addTriggerNotification(assetType: AssetType, callBack: => Unit): Unit = {
    messageHideKeyboard()
    sendInlineMessageToUser(GeneralTexts.TRIGGER_TYPE, GeneralMarkups.generateTriggerOptions(userId))
    context become waitForTriggerType(assetType, callBack)
  }

  private def waitForTriggerType(assetType: AssetType, callBack: => Unit): Receive = {
    case IncomingCallback(CallbackTypes.triggerSetType, x) =>
      val nType = TriggerNotificationType.define(x.message)
      sendMessageToUser(GeneralTexts.TRIGGER_BOUND)
      context become waitForTriggerBound(assetType, nType, callBack)
  }

  private def waitForTriggerBound(assetType: AssetType, nType: TriggerNotificationType, callBack: => Unit): Receive = {
    case IncomingMessage(floatAmount(bound)) =>
      val boundPrice = BigDecimal(bound)
      val notification = assetType match {
        case PortfolioAsset(name) => PortfolioTriggerNotification(userId, name, boundPrice, nType)
        case StockAsset(name) => StockTriggerNotification(userId, name, boundPrice, nType)
        case ExchangeRateAsset(from, to) => ExchangeRateTriggerNotification(userId, (from, to), boundPrice, nType)
      }
      userDataStorage.addTriggerNotification(notification).onComplete {
        case Success(_) =>
          sendMessageToUser(GeneralTexts.TRIGGER_ADDED)
          callBack
        case _ =>
          sendMessageToUser(GeneralTexts.TRIGGER_ADD_ERROR)
          callBack
      }
      context become waitForNewBehavior()
    case IncomingMessage(_) =>
      sendMessageToUser(GeneralTexts.TRIGGER_ADD_ERROR)
      context become waitForNewBehavior()
      callBack
  }

  //NEW DAILY NOTIFICATION
  //callback should send SetBehavior message to self to take control back
  def addDailyNotification(assetType: AssetType, callBack: => Unit): Unit = {
    userDataStorage.getUserNotification(userId, assetType) onComplete {
      case Success(notification) =>
        messageHideKeyboard()
        sendInlineMessageToUser(
          GeneralTexts.DAILY_NOTIFICATION_ADD_INFO(assetType, notification),
          GeneralMarkups.generateDailyNotificationOptions(userId)
        )
        self ! SetBehavior(waitForNotificationTime(assetType, callBack))
      case exception =>
        sendMessageToUser(GeneralTexts.ERROR)
        logger.error(s"Can't get daily notification on $assetType", exception)
        callBack
    }
    context become waitForNewBehavior()
  }

  def setNotification(userId: Long, assetType: AssetType, time: String): Future[Unit] = {
    val localTime: LocalTime = LocalTime.parse(time, DateTimeFormatter.ofPattern("H:mm"))
    val notification = assetType match {
      case PortfolioAsset(name) => PortfolioDailyNotification(userId, name, localTime)
      case StockAsset(name) => StockDailyNotification(userId, name, localTime)
      case ExchangeRateAsset(from, to) => ExchangeRateDailyNotification(userId, (from, to), localTime)
    }
    val task = for {_ <- clearNotification(userId, assetType)
                    _ = dailyNotification.addDailyNotification(notification)
                    _ <- userDataStorage.addDailyNotification(notification)
    } yield {
      sendMessageToUser(GeneralTexts.DAILY_NOTIFICATION_SET(time))
      ()
    }
    task.recoverWith {
      case exception: DateTimeParseException =>
        sendMessageToUser(GeneralTexts.TIME_ERROR)
        Future.failed(exception)
    }
  }

  def clearNotification(userId: Long, assetType: AssetType): Future[Unit] = {
    for {userNotOpt <- userDataStorage.getUserNotification(userId, assetType)
    } yield for {userNot <- userNotOpt
                 _ = dailyNotification.deleteDailyNotification(userNot)
    } yield for {_ <- userDataStorage.deleteDailyNotification(userNot)
    } yield ()
  }

  private def waitForNotificationTime(assetType: AssetType, callBack: => Unit): Receive = {
    case IncomingCallback(CallbackTypes.notificationTime, x) => x.message match {
      case Buttons.notificationReject =>
        clearNotification(userId, assetType).onComplete {
          case Success(()) => callBack
          case Failure(e) =>
            logger.error("Can't clear notification", e)
            callBack
        }
        sendMessageToUser(GeneralTexts.DAILY_NOTIFICATION_UNSET)
        context become waitForNewBehavior()
      case time: String =>
        setNotification(userId, assetType, time).onComplete {
          case Success(()) => callBack
          case Failure(e) =>
            logger.error("Can't set notification", e)
            callBack
        }
        context become waitForNewBehavior()
    }
    case IncomingMessage(time) =>
      setNotification(userId, assetType, time).onComplete {
        case Success(()) => callBack
        case Failure(e) =>
          logger.error("Can't set notification", e)
          callBack
      }
      context become waitForNewBehavior()
    case _ =>
      sendMessageToUser(GeneralTexts.TIME_ERROR)
      context become waitForNewBehavior()
      callBack
  }
}

object MainStuff {

  val floatAmount: Regex = "([0-9]+[.]?[0-9]*)".r
}



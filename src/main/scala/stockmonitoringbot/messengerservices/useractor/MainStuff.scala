package stockmonitoringbot.messengerservices.useractor

import java.time._
import java.time.format.{DateTimeFormatter, DateTimeParseException}

import akka.actor.Actor.Receive
import akka.actor.{ActorContext, ActorRef}
import com.typesafe.scalalogging.Logger
import info.mukel.telegrambot4s.methods.SendMessage
import info.mukel.telegrambot4s.models.{InlineKeyboardMarkup, ReplyKeyboardMarkup, ReplyKeyboardRemove}
import stockmonitoringbot.datastorage.models._
import stockmonitoringbot.messengerservices.UserActorService
import stockmonitoringbot.messengerservices.markups.{Buttons, GeneralMarkups, GeneralTexts}
import stockmonitoringbot.messengerservices.useractor.UserActor.{CallbackTypes, IncomingCallback, IncomingMessage, SetBehavior}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
  * Created by amir.
  */
trait MainStuff {
  val userId: Long
  val userActorService: UserActorService
  val logger: Logger
  val self: ActorRef
  implicit val context: ActorContext
  implicit val ec: ExecutionContext

  def sendMessageToUser(message: String, markup: Option[ReplyKeyboardMarkup] = None): Unit =
    userActorService.sendMessage(SendMessage(userId, message, disableWebPagePreview = Some(true), replyMarkup = markup))

  def messageHideKeyboard(msg: String): Unit =
    userActorService.sendMessage(SendMessage(userId, msg, disableWebPagePreview = Some(true), replyMarkup = Some(ReplyKeyboardRemove())))

  def sendInlineMessageToUser(message: String, markup: Option[InlineKeyboardMarkup]): Unit =
    userActorService.sendMessage(SendMessage(userId, message, replyMarkup = markup))

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
    sendMessageToUser(GeneralTexts.TRIGGER_TYPE, GeneralMarkups.onlyBack)
    sendInlineMessageToUser(GeneralTexts.TRIGGER_TYPE_MORE, GeneralMarkups.generateTriggerOptions(userId))
    context become waitForTriggerType(assetType, callBack)
  }

  private def waitForTriggerType(assetType: AssetType, callBack: => Unit): Receive = {
    case IncomingCallback(CallbackTypes.triggerSetType, x) =>
      val nType = TriggerNotificationType.define(x.message)
      sendMessageToUser(GeneralTexts.TRIGGER_BOUND, GeneralMarkups.onlyBack)
      context become waitForTriggerBound(assetType, nType, callBack)
    case IncomingMessage(Buttons.back) =>
      context become waitForNewBehavior()
      callBack
  }

  private def waitForTriggerBound(assetType: AssetType, nType: TriggerNotificationType, callBack: => Unit): Receive = {
    case IncomingMessage(floatAmount(bound)) =>
      val boundPrice = BigDecimal(bound)
      val notification = assetType match {
        case PortfolioAsset(name) => PortfolioTriggerNotification(0, userId, name, boundPrice, nType)
        case StockAsset(name) => StockTriggerNotification(0, userId, name, boundPrice, nType)
        case ExchangeRateAsset(from, to) => ExchangeRateTriggerNotification(0, userId, (from, to), boundPrice, nType)
      }
      userActorService.addTriggerNotification(notification).onComplete {
        case Success(_) =>
          sendMessageToUser(GeneralTexts.TRIGGER_ADDED)
          callBack
        case _ =>
          sendMessageToUser(GeneralTexts.TRIGGER_ADD_ERROR)
          callBack
      }
      context become waitForNewBehavior()
    case IncomingMessage(Buttons.back) =>
      context become waitForNewBehavior()
      callBack
    case IncomingMessage(_) =>
      sendMessageToUser(GeneralTexts.TRIGGER_ADD_ERROR)
      context become waitForNewBehavior()
      callBack
  }

  //NEW DAILY NOTIFICATION
  //callback should send SetBehavior message to self to take control back
  def addDailyNotification(assetType: AssetType, callBack: => Unit): Unit = {
    val notF = userActorService.getUserNotificationOnAsset(userId, assetType)
    val userF = userActorService.getUser(userId)
    val infoF = for (not <- notF; user <- userF) yield (not, user.get)
    infoF onComplete {
      case Success((notification, user)) =>
        sendMessageToUser(GeneralTexts.DAILY_NOTIFICATION_ADD_INFO_INTRO(assetType, user), GeneralMarkups.onlyBack)
        sendInlineMessageToUser(
          GeneralTexts.DAILY_NOTIFICATION_ADD_INFO(notification, user),
          GeneralMarkups.generateDailyNotificationOptions(userId)
        )
        self ! SetBehavior(waitForNotificationTime(assetType, user, callBack))
      case exception =>
        sendMessageToUser(GeneralTexts.ERROR)
        logger.error(s"Can't get daily notification on $assetType", exception)
        callBack
    }
    context become waitForNewBehavior()
  }

  def setNotification(userId: Long, assetType: AssetType, time: String, user: User): Future[Unit] = {
    val notificationTry = Try {
      val localTime: LocalTime = LocalTime.parse(time, DateTimeFormatter.ofPattern("H:mm"))
      val utcTime = getTimeInUTC(localTime, user.timeZone)
      assetType match {
        case PortfolioAsset(name) => PortfolioDailyNotification(0, userId, name, utcTime)
        case StockAsset(name) => StockDailyNotification(0, userId, name, utcTime)
        case ExchangeRateAsset(from, to) => ExchangeRateDailyNotification(0, userId, (from, to), utcTime)
      }
    }
    val task = for {notification <- Future.fromTry(notificationTry)
                    _ <- clearNotification(userId, assetType)
                    _ <- userActorService.addDailyNotification(notification)
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
    userActorService.getUserNotificationOnAsset(userId, assetType).flatMap { userNotOpt =>
      userNotOpt.fold(Future.successful(())) { not =>
        userActorService.deleteDailyNotification(not.id)
      }
    }
  }

  private def waitForNotificationTime(assetType: AssetType, user: User, callBack: => Unit): Receive = {
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
        setNotification(userId, assetType, time, user).onComplete {
          case Success(()) => callBack
          case Failure(e) =>
            logger.error("Can't set notification", e)
            callBack
        }
        context become waitForNewBehavior()
    }
    case IncomingMessage(Buttons.back) =>
      context become waitForNewBehavior()
      callBack
    case IncomingMessage(time) =>
      setNotification(userId, assetType, time, user).onComplete {
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



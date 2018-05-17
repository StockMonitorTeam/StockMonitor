package stockmonitoringbot.messengerservices.useractor

import java.time.ZoneId

import akka.actor.Actor.Receive
import stockmonitoringbot.datastorage.models.{DailyNotification, TriggerNotification, User}
import stockmonitoringbot.messengerservices.markups.{Buttons, GeneralMarkups, GeneralTexts}
import stockmonitoringbot.messengerservices.useractor.UserActor.{CallbackTypes, IncomingCallback, IncomingMessage, SetBehavior}

import scala.util.{Failure, Success, Try}

/**
  * Created by amir.
  */
trait Settings {
  this: MainStuff =>

  def becomeSettingsMainMenu(): Unit = {
    sendMessageToUser(GeneralTexts.SETTINGS_INTRO_MESSAGE, GeneralMarkups.settingsMenuMarkup)
    context become settingsMainMenu
  }

  //#5
  def settingsMainMenu: Receive = {
    case IncomingMessage(Buttons.triggers) =>
      becomeTriggersMenu()
      context become waitForNewBehavior()
    case IncomingMessage(Buttons.notifications) =>
      becomeDailyNotificationsMenu()
      context become waitForNewBehavior()
    case IncomingMessage(Buttons.timezone) =>
      becomeTimezoneMenu()
      context become waitForNewBehavior()
    case IncomingMessage(Buttons.info) => sendMessageToUser(GeneralTexts.INFO_MESSAGE)
    case IncomingMessage(Buttons.backToMain) => becomeMainMenu()
  }

  def becomeTriggersMenu(): Unit = {
    userDataStorage.getUsersTriggerNotifications(userId).onComplete {
      case Success(triggers) =>
        sendMessageToUser(GeneralTexts.TRIGGERS_LIST(triggers), GeneralMarkups.triggersMenuMarkup)
        self ! SetBehavior(triggersMenu(triggers))
      case Failure(e) =>
        logger.error("Can't get triggers", e)
        sendMessageToUser(GeneralTexts.ERROR, GeneralMarkups.onlyMainMenu)
        self ! SetBehavior(triggersMenu(Seq()))
    }
  }

  //#9
  def triggersMenu(triggers: Seq[TriggerNotification]): Receive = {
    case IncomingMessage(Buttons.triggerRemove) =>
      sendMessageToUser(GeneralTexts.TRIGGER_REMOVE, GeneralMarkups.onlyBack)
      sendInlineMessageToUser(GeneralTexts.TRIGGER_TEXT_ACTIVE,
        GeneralMarkups.generateTriggersDelete(userId, triggers))
      context become waitForTriggerToDelete
    case IncomingMessage(Buttons.settings) => becomeSettingsMainMenu()
    case IncomingMessage(Buttons.backToMain) => becomeMainMenu()
  }

  def waitForTriggerToDelete: Receive = {
    case IncomingMessage(Buttons.back) => becomeTriggersMenu()
    case IncomingCallback(CallbackTypes.deleteTrigger, message) =>
      Try(message.message.toLong) match {
        case Success(id) =>
          userDataStorage.deleteTriggerNotification(id).onComplete {
            case Success(()) =>
              sendMessageToUser(GeneralTexts.TRIGGER_REMOVED)
              becomeTriggersMenu()
            case Failure(e) =>
              sendMessageToUser(GeneralTexts.ERROR)
              logger.error("Can't delete trigger", e)
              becomeTriggersMenu()
          }
        case _ =>
          sendMessageToUser(GeneralTexts.ERROR)
          becomeTriggersMenu()
      }
      context become waitForNewBehavior()
  }

  def becomeDailyNotificationsMenu(): Unit = {
    val dNotsF = userDataStorage.getUsersDailyNotifications(userId)
    val userF = userDataStorage.getUser(userId)
    val info = for (dNots <- dNotsF; user <- userF) yield (dNots, user)
    info.onComplete {
      case Success((notifications, user)) =>
        sendMessageToUser(GeneralTexts.DAILY_NOTIFICATIONS_LIST(notifications, user.get),
          GeneralMarkups.dailyNotificationsMenuMarkup)
        self ! SetBehavior(dailyNotificationsMenu(notifications))
      case Failure(e) =>
        logger.error("Can't get daily notifications", e)
        sendMessageToUser(GeneralTexts.ERROR, GeneralMarkups.onlyMainMenu)
        self ! SetBehavior(dailyNotificationsMenu(Seq()))
    }
  }

  //#10
  def dailyNotificationsMenu(notifications: Seq[DailyNotification]): Receive = {
    case IncomingMessage(Buttons.notificationDel) =>
      sendMessageToUser(GeneralTexts.DAILY_NOTIFICATION_REMOVE, GeneralMarkups.onlyBack)
      sendInlineMessageToUser(GeneralTexts.DAILY_NOTIFICATION_TEXT_ACTIVE,
        GeneralMarkups.generateDailyNotificationsDelete(userId, notifications))
      context become waitForDailyNotificationToDelete
    case IncomingMessage(Buttons.settings) => becomeSettingsMainMenu()
    case IncomingMessage(Buttons.backToMain) => becomeMainMenu()
  }

  def waitForDailyNotificationToDelete: Receive = {
    case IncomingMessage(Buttons.back) =>
      becomeDailyNotificationsMenu()
      context become waitForNewBehavior()
    case IncomingCallback(CallbackTypes.deleteDailyNot, message) =>
      Try(message.message.toLong) match {
        case Success(id) =>
          userDataStorage.deleteDailyNotification(id).onComplete {
            case Success(()) =>
              sendMessageToUser(GeneralTexts.DAILY_NOTIFICATION_REMOVED)
              becomeDailyNotificationsMenu()
            case Failure(e) =>
              sendMessageToUser(GeneralTexts.ERROR)
              logger.error("Can't delete daily notification", e)
              becomeDailyNotificationsMenu()
          }
        case _ =>
          sendMessageToUser(GeneralTexts.ERROR)
          becomeDailyNotificationsMenu()
      }
      context become waitForNewBehavior()
  }

  def becomeTimezoneMenu(): Unit = {
    userDataStorage.getUser(userId).onComplete {
      case Success(Some(user)) =>
        sendMessageToUser(GeneralTexts.TIME_ZONE_SHOW(user), GeneralMarkups.timezoneMenuMarkup)
        self ! SetBehavior(timezoneMenu)
      case Success(None) =>
        logger.error(s"Can't find user $userId")
        sendMessageToUser(GeneralTexts.ERROR, GeneralMarkups.onlyMainMenu)
        self ! SetBehavior(timezoneMenu)
      case Failure(e) =>
        logger.error("Can't get timezone", e)
        sendMessageToUser(GeneralTexts.ERROR, GeneralMarkups.onlyMainMenu)
        self ! SetBehavior(timezoneMenu)
    }
    context become waitForNewBehavior()
  }

  //#8
  def timezoneMenu: Receive = {
    case IncomingMessage(Buttons.timezoneChange) =>
      sendMessageToUser(GeneralTexts.TIME_ZONE_CHANGE, GeneralMarkups.onlyBack)
      context become waitForNewTimezone
    case IncomingMessage(Buttons.settings) => becomeSettingsMainMenu()
    case IncomingMessage(Buttons.backToMain) => becomeMainMenu()
  }

  def waitForNewTimezone: Receive = {
    case IncomingMessage(Buttons.back) =>
      becomeTimezoneMenu()
      context become waitForNewBehavior()
    case IncomingMessage(timezone(zone, _)) =>
      Try(ZoneId.of(if (zone == "0") "+0" else zone)) match {
        case Success(zoneId) =>
          userDataStorage.setUser(User(userId, zoneId)).onComplete {
            case Success(()) =>
              sendMessageToUser(GeneralTexts.TIME_ZONE_CHANGED)
              becomeTimezoneMenu()
            case Failure(e) =>
              sendMessageToUser(GeneralTexts.ERROR)
              logger.error("Can't change time zone", e)
              becomeTimezoneMenu()
          }
        case _ =>
          sendMessageToUser(GeneralTexts.ERROR)
          becomeTimezoneMenu()
      }
      context become waitForNewBehavior()
    case IncomingMessage(_) =>
      sendMessageToUser(GeneralTexts.WRONG_TIMEZONE_FORMAT)
  }

}

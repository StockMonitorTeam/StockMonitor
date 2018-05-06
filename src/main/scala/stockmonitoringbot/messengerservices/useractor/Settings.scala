package stockmonitoringbot.messengerservices.useractor

import akka.actor.Actor.Receive
import stockmonitoringbot.datastorage.models.{DailyNotification, TriggerNotification}
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
    case IncomingMessage(Buttons.triggers) => becomeTriggersMenu()
    case IncomingMessage(Buttons.notifications) => becomeDailyNotificationsMenu()
    case IncomingMessage(Buttons.timezone) => sendMessageToUser(GeneralTexts.UNIMPLEMENTED)
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
    context become waitForNewBehavior()
  }

  //#9
  def triggersMenu(triggers: Seq[TriggerNotification]): Receive = {
    case IncomingMessage(Buttons.triggerRemove) =>
      sendMessageToUser("23", GeneralMarkups.onlyBack)
      sendInlineMessageToUser(GeneralTexts.TRIGGER_REMOVE,
        GeneralMarkups.generateTriggersDelete(userId, triggers))
      context become waitForTriggerToDelete(triggers)
    case IncomingMessage(Buttons.settings) => becomeSettingsMainMenu()
    case IncomingMessage(Buttons.backToMain) => becomeMainMenu()
  }

  def waitForTriggerToDelete(triggers: Seq[TriggerNotification]): Receive = {
    case IncomingMessage(Buttons.back) => becomeTriggersMenu()
    case IncomingCallback(CallbackTypes.deleteTrigger, message) =>
      Try(message.message.toInt) match {
        case Success(id) =>
          userDataStorage.deleteTriggerNotification(triggers(id)).onComplete {
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
    userDataStorage.getUsersDailyNotifications(userId).onComplete {
      case Success(notifications) =>
        sendMessageToUser(GeneralTexts.DAILY_NOTIFICATIONS_LIST(notifications), GeneralMarkups.dailyNotificationsMenuMarkup)
        self ! SetBehavior(dailyNotificationsMenu(notifications))
      case Failure(e) =>
        logger.error("Can't get daily notifications", e)
        sendMessageToUser(GeneralTexts.ERROR, GeneralMarkups.onlyMainMenu)
        self ! SetBehavior(dailyNotificationsMenu(Seq()))
    }
    context become waitForNewBehavior()
  }

  //#10
  def dailyNotificationsMenu(notifications: Seq[DailyNotification]): Receive = {
    case IncomingMessage(Buttons.notificationDel) =>
      sendMessageToUser("23", GeneralMarkups.onlyBack)
      sendInlineMessageToUser(GeneralTexts.DAILY_NOTIFICATION_REMOVE,
        GeneralMarkups.generateDailyNotificationsDelete(userId, notifications))
      context become waitForDailyNotificationToDelete(notifications)
    case IncomingMessage(Buttons.settings) => becomeSettingsMainMenu()
    case IncomingMessage(Buttons.backToMain) => becomeMainMenu()
  }

  def waitForDailyNotificationToDelete(notifications: Seq[DailyNotification]): Receive = {
    case IncomingMessage(Buttons.back) => becomeDailyNotificationsMenu()
    case IncomingCallback(CallbackTypes.deleteDailyNot, message) =>
      Try(message.message.toInt) match {
        case Success(id) =>
          userDataStorage.deleteDailyNotification(notifications(id)).onComplete {
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

}

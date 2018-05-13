package stockmonitoringbot.messengerservices.useractor

import akka.actor.Actor.Receive
import akka.actor.{Actor, Props}
import com.typesafe.scalalogging.Logger
import stockmonitoringbot.datastorage._
import stockmonitoringbot.messengerservices.MessageSenderComponent.MessageSender
import stockmonitoringbot.messengerservices.markups.{Buttons, GeneralMarkups, GeneralTexts}
import stockmonitoringbot.messengerservices.useractor.UserActor.{NewUser, RestartingUser, UserType}
import stockmonitoringbot.notificationhandlers.DailyNotificationHandler
import stockmonitoringbot.stocksandratescache.PriceCache

import scala.concurrent.ExecutionContextExecutor

/**
  * Created by amir.
  */
class UserActor(val userId: Long,
                val userType: UserType,
                val messageSender: MessageSender,
                val userDataStorage: UserDataStorage,
                val dailyNotification: DailyNotificationHandler,
                val cache: PriceCache) extends Actor
  with MainStuff
  with Stocks
  with Settings
  with ExchangeRates
  with Portfolios {

  override implicit lazy val ec: ExecutionContextExecutor = context.dispatcher

  override val logger: Logger = Logger(getClass)

  import UserActor._

  override def preStart(): Unit = {
    userType match {
      case NewUser => sendMessageToUser(GeneralTexts.INTRO_MESSAGE, GeneralMarkups.startMenuMarkup)
      case RestartingUser => ()
    }
  }

  override def receive: Receive = userType match {
    case NewUser => mainMenu
    case RestartingUser => waitForAnyMessage
  }

  override def becomeMainMenu(): Unit = {
    sendMessageToUser(GeneralTexts.MAIN_MENU_GREETING, GeneralMarkups.startMenuMarkup)
    context become mainMenu
  }

  //#1
  def mainMenu: Receive = {
    case IncomingMessage(Buttons.stock) =>
      becomeStockMainMenu()
    case IncomingMessage(Buttons.currency) =>
      becomeExchangeRatesMainMenu()
    case IncomingMessage(Buttons.portfolio) =>
      context become waitForNewBehavior()
      becomePortfolioMainMenu()
    case IncomingMessage(Buttons.settings) =>
      becomeSettingsMainMenu()
  }

  def waitForAnyMessage: Receive = {
    case _ =>
      becomeMainMenu()
  }

}

object UserActor {
  def props(id: Long, userType: UserType, messageSender: MessageSender, userDataStorage: UserDataStorage, dailyNotificationHandler: DailyNotificationHandler, cache: PriceCache): Props =
    Props(new UserActor(id, userType, messageSender, userDataStorage, dailyNotificationHandler, cache))

  case class IncomingMessage(message: String)
  case class IncomingCallback(handler: String, message: IncomingCallbackMessage)
  case class SetBehavior(behavior: Receive)

  case class IncomingCallbackMessage(userId: String, message: String)
  object CallbackTypes {
    val portfolio = "PRT"
    val notificationTime = "PRN"
    val portfolioDeleteStock = "PRD"
    val triggerSetType = "TRG"
    val portfolioDeleteTrigger = "PDT"
    val deleteTrigger = "DTR"
    val deleteDailyNot = "DDN"
    val choseCurrency = "CCY"
  }

  sealed trait UserType
  case object NewUser extends UserType
  case object RestartingUser extends UserType

}
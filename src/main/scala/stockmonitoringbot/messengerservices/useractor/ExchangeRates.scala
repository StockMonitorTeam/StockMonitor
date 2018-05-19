package stockmonitoringbot.messengerservices.useractor

import akka.actor.Actor.Receive
import stockmonitoringbot.datastorage.models.ExchangeRateAsset
import stockmonitoringbot.messengerservices.markups.{Buttons, GeneralMarkups, GeneralTexts}
import stockmonitoringbot.messengerservices.useractor.UserActor.{IncomingMessage, SetBehavior}
import stockmonitoringbot.stockpriceservices.models.CurrencyExchangeRateInfo

import scala.util.matching.Regex
import scala.util.{Failure, Success}

/**
  * Created by amir.
  */
trait ExchangeRates {
  this: MainStuff =>

  def becomeExchangeRatesMainMenu(): Unit = {
    userActorService.getExchangeRateQueryHistory(userId, 3).onComplete {
      case Success(lastQueries) if lastQueries.nonEmpty =>
        sendMessageToUser(GeneralTexts.EXCHANGE_RATE_INTRO_MESSAGE_WITH_HISTORY(lastQueries), GeneralMarkups.onlyMainMenu)
        self ! SetBehavior(waitForExchangePair)
      case _ => //todo parse Failure
        sendMessageToUser(GeneralTexts.EXCHANGE_RATE_INTRO_MESSAGE, GeneralMarkups.onlyMainMenu)
        self ! SetBehavior(waitForExchangePair)
    }
    context become waitForNewBehavior()
  }

  //#3
  def waitForExchangePair: Receive = {
    case IncomingMessage(ExchangePairName(from, to)) =>
      logger.info(s"Got message : $from, $to")
      userActorService.getExchangeRate(from, to, userId).onComplete {
        case Success(rate) =>
          goToRateMenu(rate)
        case Failure(exception) =>
          sendMessageToUser(GeneralTexts.printExchangeRateException(from, to))
          logger.warn(s"$exception", exception)
          self ! SetBehavior(waitForExchangePair)
      }
      context become waitForNewBehavior()
    case IncomingMessage(Buttons.backToMain) =>
      becomeMainMenu()
    case IncomingMessage(_) =>
      sendMessageToUser(GeneralTexts.EXCHANGE_RATE_INVALID)
  }

  def goToRateMenu(rate: CurrencyExchangeRateInfo): Unit = {
    val dailyNotFut = userActorService.getUserNotificationOnAsset(userId, ExchangeRateAsset(rate.from, rate.to))
    val triggerNotFut = userActorService.getUserTriggerNotificationOnAsset(userId, ExchangeRateAsset(rate.from, rate.to))
    val userFut = userActorService.getUser(userId)
    for {dailyNot <- dailyNotFut
         triggerNot <- triggerNotFut
         user <- userFut
    } {
      sendMessageToUser(GeneralTexts.printExchangeRate(rate, triggerNot, dailyNot, user.get),
        GeneralMarkups.oneExchangeRateMenuMarkup)
      self ! SetBehavior(exchangePairMenu(rate))
    }
  }

  //#7
  def exchangePairMenu(rate: CurrencyExchangeRateInfo): Receive = {
    case IncomingMessage(Buttons.notificationAdd) => addDailyNotification(ExchangeRateAsset(rate.from, rate.to), {
      goToRateMenu(rate)
    })
    case IncomingMessage(Buttons.triggerAdd) => addTriggerNotification(ExchangeRateAsset(rate.from, rate.to), {
      goToRateMenu(rate)
    })
    case IncomingMessage(Buttons.currency) => becomeExchangeRatesMainMenu()
    case IncomingMessage(Buttons.backToMain) => becomeMainMenu()
  }

  val ExchangePairName: Regex = "([A-Z]+)/([A-Z]+)".r

}

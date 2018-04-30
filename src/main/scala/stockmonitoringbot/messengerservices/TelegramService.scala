package stockmonitoringbot.messengerservices

import java.util.concurrent.ConcurrentHashMap

import akka.actor.{ActorRef, PoisonPill}
import info.mukel.telegrambot4s.api._
import info.mukel.telegrambot4s.api.declarative.{Callbacks, Commands}
import stockmonitoringbot.datastorage.UserDataStorageComponent
import stockmonitoringbot.messengerservices.MessageSenderComponent.MessageSender
import stockmonitoringbot.messengerservices.UserActor.{IncomingCallback, IncomingMessage}
import stockmonitoringbot.stocksandratescache.PriceCacheComponent
import stockmonitoringbot.{ActorSystemComponent, ApiKeys, ExecutionContextComponent}

import scala.util.{Failure, Success}

/**
  * Created by amir.
  */
trait TelegramService extends TelegramBot
  with Webhook
  with Commands
  with Callbacks
  with MessageSenderComponent {
  this: ExecutionContextComponent
    with ActorSystemComponent
    with UserDataStorageComponent
    with PriceCacheComponent
    with ApiKeys =>

  override val token: String = getKey("StockMonitor.Telegram.apitoken")
  override val port: Int = getKey("StockMonitor.Telegram.port").toInt
  override val webhookUrl: String = getKey("StockMonitor.Telegram.url")

  logger.info("starting telegram bot")

  private val activeUsers = new ConcurrentHashMap[Long, ActorRef]()

  onCommand("/start") {
    implicit msg =>
      logger.info(s"starting chat with ${
        msg.chat.firstName.get
      }")
      val prev = activeUsers.put(msg.chat.id,
        system.actorOf(UserActor.props(msg.chat.id, messageSender, userDataStorage, priceCache)))
      Option(prev).foreach(_ ! PoisonPill)
  }

  onMessage {
    msg =>
      logger.info(s"message in ${msg.chat.id} from ${
        msg.chat.firstName.getOrElse("")
      } : ${
        msg.text.getOrElse("")
      }")
      for {
        user <- Option(activeUsers.get(msg.chat.id))
        messageText <- msg.text
      } user ! IncomingMessage(messageText)
  }

  onCallbackQuery {
    msg =>
      logger.info(s"callback from ${
        msg.from.firstName
      } : ${
        msg.data.getOrElse("")
      }")

      for {
        message <- msg.message
        user <- Option(activeUsers.get(message.chat.id))
        messageData <- msg.data
      } messageData.split("_", 3) match {
        case Array(callbackType, userId, message) =>
          user ! IncomingCallback(callbackType, IncomingCallbackMessage(userId, message))
        case _ =>
          logger.warn(s"Callback not matched. $messageData")
      }
      ackCallback()(msg)
  }

  override val messageSender: MessageSender = message => request(message).onComplete {
    case Success(_) =>
    case Failure(exception) =>
      logger.error(s"Can't deliver message: $exception")
  }

}

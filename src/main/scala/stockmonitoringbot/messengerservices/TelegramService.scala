package stockmonitoringbot.messengerservices

import akka.actor.{ActorRef, PoisonPill}
import info.mukel.telegrambot4s.api._
import info.mukel.telegrambot4s.api.declarative.Commands
import info.mukel.telegrambot4s.methods.SendMessage
import stockmonitoringbot.datastorage.UserDataStorageComponent
import stockmonitoringbot.messengerservices.UserActor.IncomingMessage
import stockmonitoringbot.stocksandratescache.PriceCacheComponent
import stockmonitoringbot.{ActorSystemComponent, ApiKeys, ExecutionContextComponent}

import scala.collection.mutable
import scala.util.{Failure, Success}

/**
  * Created by amir.
  */
trait TelegramService extends TelegramBot
  with Webhook
  with Commands
  with MessageSender {
  self: ExecutionContextComponent
    with ActorSystemComponent
    with UserDataStorageComponent
    with PriceCacheComponent
    with ApiKeys =>

  override val token: String = getKey("StockMonitor.Telegram.apitoken")
  override val port: Int = getKey("StockMonitor.Telegram.port").toInt
  override val webhookUrl: String = getKey("StockMonitor.Telegram.url")

  logger.info("starting telegram bot")

  private val activeUsers: mutable.Map[Long, ActorRef] =
    mutable.HashMap.empty

  onCommand("/start") {
    implicit msg =>
      logger.info(s"starting chat with ${
        msg.chat.firstName.get
      }")
      activeUsers.get(msg.chat.id).foreach(_ ! PoisonPill)
      activeUsers += msg.chat.id -> system.actorOf(UserActor.props(msg.chat.id, this, userDataStorage, priceCache))
  }

  onMessage {
    msg =>
      logger.info(s"message from ${
        msg.chat.firstName.getOrElse("")
      } : ${
        msg.text.getOrElse("")
      }")
      for {
        user <- activeUsers.get(msg.chat.id)
        messageText <- msg.text
      } user ! IncomingMessage(messageText)
  }

  override def send(message: SendMessage): Unit = request(message).onComplete {
    case Success(_) =>
    case Failure(exception) =>
      logger.error(s"Can't deliver message: $exception")
  }

}

package stockmonitoringbot.messengerservices

import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap

import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.Logger
import info.mukel.telegrambot4s.api._
import info.mukel.telegrambot4s.api.declarative.{Callbacks, Commands}
import info.mukel.telegrambot4s.clients.AkkaClient
import info.mukel.telegrambot4s.methods.SendMessage
import stockmonitoringbot.datastorage.UserDataStorageComponent
import stockmonitoringbot.datastorage.models.User
import stockmonitoringbot.messengerservices.MessageSenderComponent.MessageSender
import stockmonitoringbot.messengerservices.useractor.UserActor
import stockmonitoringbot.messengerservices.useractor.UserActor.{IncomingCallback, IncomingCallbackMessage, IncomingMessage}
import stockmonitoringbot.notificationhandlers.DailyNotificationHandlerComponent
import stockmonitoringbot.stocksandratescache.PriceCacheComponent
import stockmonitoringbot.{ActorSystemComponent, AppConfig, ExecutionContextComponent}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
  * Created by amir.
  */

trait TelegramMessageReceiverAndSenderComponent extends MessageReceiverComponent with MessageSenderComponent {
  self: ExecutionContextComponent
    with ActorSystemComponent
    with UserDataStorageComponent
    with DailyNotificationHandlerComponent
    with PriceCacheComponent
    with AppConfig =>

  val tg = new TelegramMessageReceiverAndSender
  val messageReceiver: MessageReceiver = tg
  val messageSender: MessageSender = tg

  class TelegramMessageReceiverAndSender extends BotBase
    with Webhook
    with Commands
    with Callbacks
    with MessageSender
    with MessageReceiver {

    override implicit lazy val system: ActorSystem = self.system
    override implicit lazy val materializer: ActorMaterializer = self.materializer
    override implicit lazy val executionContext: ExecutionContext = self.executionContext
    override lazy val logger = Logger(getClass)
    override lazy val client: RequestHandler = new AkkaClient(token)

    override val token: String = getKey("StockMonitor.Telegram.apitoken")
    override val port: Int = getKey("StockMonitor.Telegram.port").toInt
    override val webhookUrl: String = getKey("StockMonitor.Telegram.url")
    private val defaultTimeZone: ZoneId = ZoneId.of(getKey("StockMonitor.defaultTimezone"))

    private val activeUsers = new ConcurrentHashMap[Long, ActorRef]()

    logger.info("starting telegram bot")

    onCommand("/start") {
      implicit msg =>
        logger.info(s"starting chat with ${msg.chat.firstName.get}")
        for {user <- userDataStorage.getUser(msg.chat.id) if user.isEmpty
             _ <- userDataStorage.setUser(User(msg.chat.id, defaultTimeZone))
        } {}
        val prev = activeUsers.put(msg.chat.id,
          system.actorOf(UserActor.props(msg.chat.id, messageSender, userDataStorage, dailyNotificationHandler, priceCache)))
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
          case Array(callbackType, userId, text) =>
            user ! IncomingCallback(callbackType, IncomingCallbackMessage(userId, text))
          case _ =>
            logger.warn(s"Callback not matched. $messageData")
        }
        ackCallback()(msg)
        ()
    }

    override def apply(message: SendMessage): Unit =
      request(message).onComplete {
        case Success(_) =>
        case Failure(exception) =>
          logger.error(s"Can't deliver message", exception)
      }
    override def startReceiving(): Unit = run()
    override def stopReceiving(): Future[Unit] = shutdown()
  }

}


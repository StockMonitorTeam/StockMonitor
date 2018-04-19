package stockmonitoringbot.messengerservices

import com.typesafe.scalalogging.Logger
import info.mukel.telegrambot4s.api._
import info.mukel.telegrambot4s.clients.AkkaClient

/**
  * Created by amir.
  */
//this trait was implemented to avoid creating ActorSystem by default TelegramBot
trait TelegramBot extends BotBase with AkkaImplicits with BotExecutionContext {
  override lazy val logger = Logger(getClass)
  override lazy val client: RequestHandler = new AkkaClient(token)
}

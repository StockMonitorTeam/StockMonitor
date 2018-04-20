package stockmonitoringbot.messengerservices

import com.typesafe.scalalogging.Logger
import info.mukel.telegrambot4s.api._
import info.mukel.telegrambot4s.clients.AkkaClient
import stockmonitoringbot.{ActorSystemComponent, ExecutionContextComponent}

/**
  * Created by amir.
  */
//this trait was implemented to avoid creating ActorSystem by default TelegramBot
trait TelegramBot extends BotBase {
  self : ExecutionContextComponent with ActorSystemComponent =>
  override lazy val logger = Logger(getClass)
  override lazy val client: RequestHandler = new AkkaClient(token)
}
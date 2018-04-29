package stockmonitoringbot.messengerservices

import info.mukel.telegrambot4s.methods.SendMessage
import stockmonitoringbot.messengerservices.MessageSenderComponent.MessageSender

/**
  * Created by amir.
  */
trait MessageSenderComponent {
  val messageSender: MessageSender
}

object MessageSenderComponent {
  type MessageSender = SendMessage => Unit
}

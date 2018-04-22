package stockmonitoringbot.messengerservices

import info.mukel.telegrambot4s.methods.SendMessage

/**
  * Created by amir.
  */
trait MessageSender {
  def send(message: SendMessage): Unit
}
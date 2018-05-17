package stockmonitoringbot.messengerservices

import scala.concurrent.Future

/**
  * Created by amir.
  */
trait MessageReceiverComponent {
  val messageReceiver: MessageReceiver
}

trait MessageReceiver {
  def startReceiving(): Future[Unit]
  def stopReceiving(): Future[Unit]
}
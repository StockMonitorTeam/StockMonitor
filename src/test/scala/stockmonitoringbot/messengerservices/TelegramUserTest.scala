package stockmonitoringbot.messengerservices

import akka.actor.ActorSystem
import info.mukel.telegrambot4s.methods.SendMessage
import info.mukel.telegrambot4s.models.ChatId
import org.scalatest.{FlatSpec, Matchers}
import stockmonitoringbot.ExecutionContextComponent
import stockmonitoringbot.datastorage.InMemoryDataStorage
import stockmonitoringbot.messengerservices.UserActor.IncomingMessage

import scala.concurrent.ExecutionContext

/**
  * Created by amir.
  */
class TelegramUserTest extends FlatSpec with Matchers {

  var outgoingMessages: Seq[SendMessage] = Seq()

  val stock = "MSFT"

  private val messageSenderMock = new {} with MessageSender {
    override def send(message: SendMessage): Unit =
      outgoingMessages = message +: outgoingMessages
  }

  private val system = ActorSystem()

  private val dataBaseMock = new {}
    with InMemoryDataStorage
    with ExecutionContextComponent {
    override implicit val executionContext: ExecutionContext = system.dispatcher
  }

  "UserActor" should "send hello message on creating" in {
    system.actorOf(UserActor.props(0, messageSenderMock, dataBaseMock))
    Thread.sleep(100)
    outgoingMessages.head shouldBe SendMessage(ChatId(0), "Hello, choose action:", replyMarkup = UserActor.startMenuMarkup)
  }

  "UserActor" should "return stock price" in {
    dataBaseMock.addStock(stock, 23)
    val user = system.actorOf(UserActor.props(0, messageSenderMock, dataBaseMock))
    user ! IncomingMessage(UserActor.getStockPrice)
    user ! IncomingMessage("MSFT")
    Thread.sleep(100)
    outgoingMessages.tail.head shouldBe SendMessage(ChatId(0), s"$stock current price is 23.0")
  }


}

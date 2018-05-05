package stockmonitoringbot.messengerservices

import akka.actor.ActorSystem
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by amir.
  */
class TelegramUserTest extends FlatSpec with Matchers with MockFactory {

  val stock = "MSFT"

  private val system = ActorSystem()
  //todo fix tests
  /*private val dataBaseMock = new {}
    with InMemoryDataStorage
    with ExecutionContextComponent {
    override implicit val executionContext: ExecutionContext = system.dispatcher
  }

  "UserActor" should "send hello message on creating" in {
    system.actorOf(UserActor.props(0, messageSenderMock, dataBaseMock))
    Thread.sleep(100)
    outgoingMessages.head shouldBe SendMessage(ChatId(0), GeneralTexts.INTRO_MESSAGE, replyMarkup = GeneralMarkups.startMenuMarkup)
  }

  "UserActor" should "return stock price" in {
    dataBaseMock.addStock(stock, 23)
    val user = system.actorOf(UserActor.props(0, messageSenderMock, dataBaseMock))
    user ! IncomingMessage(Buttons.stock)
    user ! IncomingMessage("MSFT")
    Thread.sleep(100)

    outgoingMessages.head shouldBe SendMessage(ChatId(0), GeneralTexts.printStockPrice("MSFT", 23.0))
  }
  */

}

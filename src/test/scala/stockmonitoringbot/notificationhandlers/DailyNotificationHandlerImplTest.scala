package stockmonitoringbot.notificationhandlers

import info.mukel.telegrambot4s.methods.SendMessage
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}
import stockmonitoringbot.datastorage.{UserDataStorage, UserDataStorageComponent}
import stockmonitoringbot.messengerservices.MessageSenderComponent
import stockmonitoringbot.stocksandratescache.{PriceCache, PriceCacheComponent}
import stockmonitoringbot.{ActorSystemComponentImpl, ExecutionContextImpl}

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by amir.
  */
class DailyNotificationHandlerImplTest extends FlatSpec with Matchers with ScalaFutures with MockFactory {

  private trait TestWiring extends DailyNotificationHandlerComponentImpl
    with ActorSystemComponentImpl
    with ExecutionContextImpl
    with MessageSenderComponent
    with UserDataStorageComponent
    with PriceCacheComponent {
    implicit val patienceConfig: PatienceConfig = PatienceConfig(500 millis, 20 millis)
    override val messageSender = mockFunction[SendMessage, Unit]
    override val priceCache = mock[PriceCache]
    override val userDataStorage = mock[UserDataStorage]
  }

  //todo
}

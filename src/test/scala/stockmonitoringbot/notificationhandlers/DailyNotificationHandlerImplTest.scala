package stockmonitoringbot.notificationhandlers

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import info.mukel.telegrambot4s.methods.SendMessage
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures
import stockmonitoringbot.datastorage.InMemoryUserDataStorage
import stockmonitoringbot.messengerservices.MessageSender
import stockmonitoringbot.stocksandratescache.StocksAndExchangeRatesCache
import stockmonitoringbot.{ActorSystemComponentImpl, ExecutionContextImpl}

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by amir.
  */
class DailyNotificationHandlerImplTest extends FlatSpec with Matchers with ScalaFutures with MockFactory {

  private trait TestWiring {
    implicit val patienceConfig: PatienceConfig = PatienceConfig(500 millis, 20 millis)
    val dailyNotificationMock = new DailyNotificationHandlerImpl
      with ActorSystemComponentImpl
      with ExecutionContextImpl
      with MessageSender
      with InMemoryUserDataStorage
      with StocksAndExchangeRatesCache//todo
      /*
       */ {
      val sendMessageMock = mockFunction[SendMessage, Unit]
      override def send(message: SendMessage): Unit = sendMessageMock(message)
    }
  }

}

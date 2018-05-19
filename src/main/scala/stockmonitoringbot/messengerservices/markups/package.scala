package stockmonitoringbot.messengerservices

import java.time._
import java.time.format.DateTimeFormatter

import stockmonitoringbot.datastorage.models._

/**
  * Created by amir.
  */
package object markups {

  def currentTimeAccordingToTimezone(zoneId: ZoneId): String = {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    LocalDateTime.now(zoneId).format(formatter)
  }

  def tnToStr(trigger: TriggerNotification): String = {
    trigger match {
      case StockTriggerNotification(_, _, name, bound, nType) =>
        s"Акции $name $nType - $bound"
      case ExchangeRateTriggerNotification(_, _, (from, to), bound, nType) =>
        s"Курс $from/$to $nType - $bound"
      case PortfolioTriggerNotification(_, _, name, bound, nType) =>
        s"Портфель $name $nType - $bound"
    }
  }
  def dnToStr(notification: DailyNotification): String = {
    notification match {
      case StockDailyNotification(_, _, name, time) =>
        s"Акции $name в $time"
      case ExchangeRateDailyNotification(_, _, (from, to), time) =>
        s"Курс $from/$to в $time"
      case PortfolioDailyNotification(_, _, name, time) =>
        s"Портфель $name в $time"
    }
  }

  def getTimeInSpecifiedTimezone(localTime: LocalTime, zoneId: ZoneId): LocalTime = {
    LocalDateTime.of(LocalDate.now(), localTime)
      .atZone(ZoneOffset.UTC)
      .withZoneSameInstant(zoneId)
      .toLocalTime
  }

  def notificationToUsersTime(not: DailyNotification, zoneId: ZoneId): DailyNotification = {
    not match {
      case t: StockDailyNotification => t.copy(time = getTimeInSpecifiedTimezone(t.time, zoneId))
      case t: PortfolioDailyNotification => t.copy(time = getTimeInSpecifiedTimezone(t.time, zoneId))
      case t: ExchangeRateDailyNotification => t.copy(time = getTimeInSpecifiedTimezone(t.time, zoneId))
    }
  }

}

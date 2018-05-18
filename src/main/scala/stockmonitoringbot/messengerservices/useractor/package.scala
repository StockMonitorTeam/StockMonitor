package stockmonitoringbot.messengerservices

import java.time._

import scala.util.matching.Regex
import java.time.format.DateTimeFormatter

/**
  * Created by amir.
  */
package object useractor {

  def currentTimeAccordingToTimezone(zoneId: ZoneId): String = {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    LocalDateTime.now(zoneId).format(formatter)
  }

  def getTimeInUTC(localTime: LocalTime, zoneId: ZoneId): LocalTime = {
    LocalDateTime.of(LocalDate.now(), localTime)
      .atZone(zoneId)
      .withZoneSameInstant(ZoneOffset.UTC)
      .toLocalTime
  }

  val stockName: Regex = "/?([A-Z]+)".r
  val floatAmount: Regex = "([0-9]+[.]?[0-9]*)".r
  val timezone: Regex = "(0|[+-]([0-9]|10|11|12))".r
}

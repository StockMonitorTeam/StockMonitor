package stockmonitoringbot.messengerservices

import java.time.{LocalDateTime, ZoneId}

import scala.util.matching.Regex

/**
  * Created by amir.
  */
package object useractor {

  def currentTimeAccordingToTimezone(zoneId: ZoneId): String = {
    LocalDateTime.now(zoneId).toString
  }

  val stockName: Regex = "/?([A-Z]+)".r
  val floatAmount: Regex = "([0-9]+[.]?[0-9]*)".r
  val timezone: Regex = "(0|[+-]([0-9]|10|11|12))".r
}

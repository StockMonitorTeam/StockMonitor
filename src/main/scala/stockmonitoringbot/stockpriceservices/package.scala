package stockmonitoringbot

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZonedDateTime}
import java.util.TimeZone

/**
  * Created by amir.
  */
package object stockpriceservices {
  def parseZonedDateTime(date: String, zone: String): ZonedDateTime = {
    val dateParser: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    val zoneId = TimeZone.getTimeZone(zone).toZoneId
    val localTime = LocalDateTime.from(dateParser.parse(date))
    ZonedDateTime.of(localTime, zoneId)
  }
}

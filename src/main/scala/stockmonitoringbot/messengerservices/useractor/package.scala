package stockmonitoringbot.messengerservices

import scala.util.matching.Regex

/**
  * Created by amir.
  */
package object useractor {
  val stockName: Regex = "/?([A-Z]+)".r
  val floatAmount: Regex = "([0-9]+[.]?[0-9]*)".r
}

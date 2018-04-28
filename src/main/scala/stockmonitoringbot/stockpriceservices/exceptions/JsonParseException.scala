package stockmonitoringbot.stockpriceservices.exceptions

import spray.json.JsValue

/**
  * Created by amir.
  */
class JsonParseException(private val failedJson: JsValue,
                         private val cause: Throwable = null)
  extends Exception(s"Failed to parse $failedJson", cause)

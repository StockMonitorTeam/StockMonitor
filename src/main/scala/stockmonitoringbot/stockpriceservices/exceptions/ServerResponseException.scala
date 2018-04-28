package stockmonitoringbot.stockpriceservices.exceptions

import akka.http.scaladsl.model.StatusCode

/**
  * Created by amir.
  */
class ServerResponseException(statusCode: StatusCode) extends Exception(s"Server response is: $statusCode")
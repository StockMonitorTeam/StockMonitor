package stockmonitoringbot.stockpriceservices

import java.time.ZonedDateTime

/**
  * Created by amir.
  */
case class CurrencyExchangeRateInfo(from: String,
                                    descriptionFrom: String,
                                    to: String,
                                    descriptionTo: String,
                                    rate: Double,
                                    lastRefreshed: ZonedDateTime)

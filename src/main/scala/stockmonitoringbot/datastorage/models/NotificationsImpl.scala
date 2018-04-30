package stockmonitoringbot.datastorage.models

import java.time.LocalTime

/**
  * Created by amir.
  */
case class PortfolioDailyNotification(ownerId: Long,
                                      portfolioName: String,
                                      time: LocalTime
                                     ) extends PortfolioNotification with DailyNotification

case class ExchangeRateDailyNotification(ownerId: Long,
                                         exchangePair: (String, String),
                                         time: LocalTime
                                        ) extends ExchangeRateNotification with DailyNotification

case class StockDailyNotification(ownerId: Long,
                                  stock: String,
                                  time: LocalTime
                                 ) extends StockNotification with DailyNotification

case class PortfolioTriggerNotification(ownerId: Long,
                                        portfolioName: String,
                                        boundPrice: BigDecimal,
                                        notificationType: TriggerNotificationType
                                       ) extends PortfolioNotification with TriggerNotification

case class ExchangeRateTriggerNotification(ownerId: Long,
                                           exchangePair: (String, String),
                                           boundPrice: BigDecimal,
                                           notificationType: TriggerNotificationType
                                          ) extends ExchangeRateNotification with TriggerNotification

case class StockTriggerNotification(ownerId: Long,
                                    stock: String,
                                    boundPrice: BigDecimal,
                                    notificationType: TriggerNotificationType
                                   ) extends StockNotification with TriggerNotification


package stockmonitoringbot.datastorage.postgresdb

import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._

/**
  * Created by amir.
  */
trait PostgresDBConnectionComponent {
  val dbConnection: PostgresProfile.backend.Database
}

trait PostgresDBConnectionComponentImpl extends PostgresDBConnectionComponent {
  override lazy val dbConnection = Database.forConfig("StockMonitor.postgresDB")
}
package stockmonitoringbot.datastorage.models

import java.sql.Timestamp

/**
  * Created by amir.
  */
case class UserQuery(userId: Long, assetType: AssetType, time: Timestamp)
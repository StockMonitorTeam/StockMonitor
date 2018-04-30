package stockmonitoringbot.datastorage

import stockmonitoringbot.ExecutionContextComponent
import stockmonitoringbot.datastorage.models._

import scala.collection.mutable
import scala.concurrent.Future

/**
  * Created by amir.
  */

trait InMemoryUserDataStorageComponentImpl extends UserDataStorageComponent {
  self: ExecutionContextComponent =>

  override val userDataStorage: UserDataStorage = new InMemoryUserDataStorage

  class InMemoryUserDataStorage extends UserDataStorage {

    private val usersTriggerNotifications: mutable.Map[Long, Set[TriggerNotification]] =
      mutable.Map.empty.withDefaultValue(Set.empty)
    private val usersDailyNotifications: mutable.Map[Long, Set[DailyNotification]] =
      mutable.Map.empty.withDefaultValue(Set.empty)
    private val usersPortfolios: mutable.Map[Long, Set[Portfolio]] =
      mutable.Map.empty.withDefaultValue(Set.empty)

    override def getUsersDailyNotifications(userId: Long): Future[Seq[DailyNotification]] =
      Future(usersDailyNotifications.synchronized {
        usersDailyNotifications(userId).toSeq
      })
    override def addDailyNotification(notification: DailyNotification): Future[Unit] =
      Future(usersDailyNotifications.synchronized {
        usersDailyNotifications(notification.ownerId) += notification
      })
    override def deleteDailyNotification(notification: DailyNotification): Future[Unit] =
      Future(usersDailyNotifications.synchronized {
        usersDailyNotifications(notification.ownerId) -= notification
      })

    override def getUsersTriggerNotifications(userId: Long): Future[Seq[TriggerNotification]] =
      Future(usersTriggerNotifications.synchronized {
        usersTriggerNotifications(userId).toSeq
      })
    override def addTriggerNotification(notification: TriggerNotification): Future[Unit] =
      Future(usersTriggerNotifications.synchronized {
        usersTriggerNotifications(notification.ownerId) += notification
      })
    override def deleteTriggerNotification(notification: TriggerNotification): Future[Unit] =
      Future(usersTriggerNotifications.synchronized {
        usersTriggerNotifications(notification.ownerId) -= notification
      })

    override def getAllTriggerNotifications: Future[Iterable[TriggerNotification]] =
      Future(usersTriggerNotifications.synchronized {
        usersTriggerNotifications.values.flatten
      })

    override def addPortfolio(portfolio: Portfolio): Future[Unit] =
      Future(usersPortfolios.synchronized {
        usersPortfolios(portfolio.userId) += portfolio
      })

    override def deletePortfolio(userId: Long, portfolioName: String): Future[Unit] =
      Future(usersPortfolios.synchronized {
        usersPortfolios(userId) = usersPortfolios(userId).filterNot(_.name == portfolioName)

        //when deleting portfolio, should delete all notifications on this portfolio
        def shouldDelete(notification: Notification): Boolean = notification match {
          case not: PortfolioNotification =>
            not.portfolioName == portfolioName
          case _ =>
            false
        }

        usersTriggerNotifications.synchronized {
          usersTriggerNotifications(userId) = usersTriggerNotifications(userId).filterNot(shouldDelete)
        }
        usersDailyNotifications.synchronized {
          usersDailyNotifications(userId) = usersDailyNotifications(userId).filterNot(shouldDelete)
        }
      })
    override def getUserPortfolios(userId: Long): Future[Seq[Portfolio]] =
      Future(usersPortfolios.synchronized {
        usersPortfolios(userId).toSeq
      })
    override def getPortfolio(userId: Long, portfolioName: String): Future[Portfolio] =
      Future(usersPortfolios.synchronized {
        usersPortfolios(userId).find(_.name == portfolioName).get
      })
    override def addStockToPortfolio(userId: Long, portfolioName: String, stock: String, count: Double): Future[Unit] =
      Future(usersPortfolios.synchronized {
        usersPortfolios(userId).find(_.name == portfolioName).foreach { oldPortfolio =>
          val newPortfolio = oldPortfolio.copy(stocks = oldPortfolio.stocks + (stock -> count))
          usersPortfolios(userId) -= oldPortfolio
          usersPortfolios(userId) += newPortfolio
        }
      })
    override def deleteStockFromPortfolio(userId: Long, portfolioName: String, stock: String): Future[Unit] =
      Future(usersPortfolios.synchronized {
        usersPortfolios(userId).find(_.name == portfolioName).foreach { oldPortfolio =>
          val newPortfolio = oldPortfolio.copy(stocks = oldPortfolio.stocks - stock)
          usersPortfolios(userId) -= oldPortfolio
          usersPortfolios(userId) += newPortfolio
        }
      })

  }
}
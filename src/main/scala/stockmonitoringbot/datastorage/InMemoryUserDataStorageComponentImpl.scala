package stockmonitoringbot.datastorage

import java.util.concurrent.ConcurrentHashMap

import stockmonitoringbot.ExecutionContextComponent
import stockmonitoringbot.datastorage.models._

import scala.concurrent.Future

/**
  * Created by amir.
  */

trait InMemoryUserDataStorageComponentImpl extends UserDataStorageComponent {
  this: ExecutionContextComponent =>

  override val userDataStorage: UserDataStorage = new InMemoryUserDataStorage

  class InMemoryUserDataStorage extends UserDataStorage {

    private val usersDailyNotifications = new ConcurrentHashMap[Long, Set[DailyNotification]]()
    private val usersTriggerNotifications = new ConcurrentHashMap[Long, Set[TriggerNotification]]()
    private val usersPortfolios = new ConcurrentHashMap[Long, Set[Portfolio]]()

    override def getUsersDailyNotifications(userId: Long): Future[Seq[DailyNotification]] =
      Future.successful(usersDailyNotifications.getOrDefault(userId, Set()).toSeq)
    override def addDailyNotification(notification: DailyNotification): Future[Unit] =
      Future.successful {
        usersDailyNotifications.compute(notification.ownerId,
          (_, notifications) => setOrEmptySet(notifications) + notification
        )
        ()
      }
    override def deleteDailyNotification(notification: DailyNotification): Future[Unit] =
      Future.successful {
        usersDailyNotifications.compute(notification.ownerId,
          (_, notifications) => setOrEmptySet(notifications) - notification
        )
        ()
      }

    override def getUsersTriggerNotifications(userId: Long): Future[Seq[TriggerNotification]] =
      Future.successful(usersTriggerNotifications.getOrDefault(userId, Set()).toSeq)
    override def addTriggerNotification(notification: TriggerNotification): Future[Unit] =
      Future.successful {
        usersTriggerNotifications.compute(notification.ownerId,
          (_, notifications) => setOrEmptySet(notifications) + notification
        )
        ()
      }
    override def deleteTriggerNotification(notification: TriggerNotification): Future[Unit] =
      Future.successful {
        usersTriggerNotifications.compute(notification.ownerId,
          (_, notifications) => setOrEmptySet(notifications) - notification
        )
        ()
      }

    override def getAllTriggerNotifications: Future[Iterable[TriggerNotification]] =
      Future.successful(
        usersTriggerNotifications.reduceValues(4, _ ++ _)
      )

    override def addPortfolio(portfolio: Portfolio): Future[Unit] =
      Future.successful {
        usersPortfolios.compute(portfolio.userId,
          (_, portfolios) => setOrEmptySet(portfolios) + portfolio
        )
        ()
      }

    override def deletePortfolio(userId: Long, portfolioName: String): Future[Unit] =
      Future.successful {
        usersPortfolios.compute(userId, (_, portfolios) => setOrEmptySet(portfolios).filterNot(_.name == portfolioName))
        //when deleting portfolio, should delete all notifications on this portfolio
        val shouldDelete: Notification => Boolean = {
          case not: PortfolioNotification => not.portfolioName == portfolioName
          case _ => false
        }
        usersTriggerNotifications.compute(userId, (_, notifications) =>
          setOrEmptySet(notifications).filterNot(shouldDelete)
        )
        usersDailyNotifications.compute(userId, (_, notifications) =>
          setOrEmptySet(notifications).filterNot(shouldDelete)
        )
        ()
      }

    override def getUserPortfolios(userId: Long): Future[Seq[Portfolio]] =
      Future.successful(usersPortfolios.getOrDefault(userId, Set()).toSeq)
    override def getPortfolio(userId: Long, portfolioName: String): Future[Portfolio] =
      Future.successful(usersPortfolios.getOrDefault(userId, Set()).find(_.name == portfolioName).get)
    override def addStockToPortfolio(userId: Long, portfolioName: String, stock: String, count: Double): Future[Unit] =
      Future.successful {
        usersPortfolios.compute(userId, (_, portfolios) => {
          val oldPortfolio = portfolios.find(_.name == portfolioName).get
          val newPortfolio = oldPortfolio.copy(stocks = oldPortfolio.stocks + (stock -> count))
          portfolios - oldPortfolio + newPortfolio
        })
        ()
      }
    override def deleteStockFromPortfolio(userId: Long, portfolioName: String, stock: String): Future[Unit] =
      Future.successful {
        usersPortfolios.compute(userId, (_, portfolios) => {
          val oldPortfolio = portfolios.find(_.name == portfolioName).get
          val newPortfolio = oldPortfolio.copy(stocks = oldPortfolio.stocks - stock)
          portfolios - oldPortfolio + newPortfolio
        })
        ()
      }

    def setOrEmptySet[A](set: Set[A]): Set[A] = if (set == null) Set() else set

  }
}
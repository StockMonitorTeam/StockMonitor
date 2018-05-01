package stockmonitoringbot.datastorage

import java.util.concurrent.ConcurrentHashMap

import stockmonitoringbot.datastorage.models._

import scala.concurrent.Future
import scala.util.Try

/**
  * Created by amir.
  */

trait InMemoryUserDataStorageComponentImpl extends UserDataStorageComponent {

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
        usersPortfolios.compute(userId, (_, portfolios) => setOrEmptySet(portfolios).filterNot(_.name == portfolioName))
        ()
      }

    override def getUserPortfolios(userId: Long): Future[Seq[Portfolio]] =
      Future.successful(usersPortfolios.getOrDefault(userId, Set()).toSeq)

    /**
      *
      * @return Failure with NoSuchElementException if there is no portfolio with specified name
      */
    override def getPortfolio(userId: Long, portfolioName: String): Future[Portfolio] =
      Future.fromTry(Try(usersPortfolios.getOrDefault(userId, Set()).find(_.name == portfolioName).get))

    /**
      *
      * @return Failure with NoSuchElementException if there is no portfolio with specified name
      */
    override def addStockToPortfolio(userId: Long, portfolioName: String, stock: String, count: Double): Future[Unit] =
      Future.fromTry(Try {
        usersPortfolios.compute(userId, (_, portfolios) => {
          val oldPortfolio = portfolios.find(_.name == portfolioName).get
          val newPortfolio = oldPortfolio.copy(stocks = oldPortfolio.stocks + (stock -> count))
          portfolios - oldPortfolio + newPortfolio
        })
        ()
      })

    /**
      *
      * @return Failure with NoSuchElementException if there is no portfolio with specified name
      */
    override def deleteStockFromPortfolio(userId: Long, portfolioName: String, stock: String): Future[Unit] =
      Future.fromTry(Try {
        usersPortfolios.compute(userId, (_, portfolios) => {
          val oldPortfolio = portfolios.find(_.name == portfolioName).get
          val newPortfolio = oldPortfolio.copy(stocks = oldPortfolio.stocks - stock)
          portfolios - oldPortfolio + newPortfolio
        })
        ()
      })
    override def getUserPortfolioNotification(userId: Long, portfolioName: String): Future[Option[PortfolioDailyNotification]] =
      Future.successful {
        usersDailyNotifications.getOrDefault(userId, Set()).collectFirst {
          case x: PortfolioDailyNotification if x.portfolioName == portfolioName => x
        }
      }
    override def deleteUserPortfolioNotification(userId: Long, portfolioName: String): Future[Unit] =
      Future.successful {
        usersDailyNotifications.getOrDefault(userId, Set()).collectFirst {
          case x: PortfolioDailyNotification if x.portfolioName == portfolioName => x
        }.foreach(n => deleteDailyNotification(n))
      }
    override def setUserPortfolioNotification(userId: Long, portfolioName: String, notification: PortfolioDailyNotification): Future[Unit] =
      Future.successful {
        deleteUserPortfolioNotification(userId, portfolioName)
        addDailyNotification(notification)
      }

    def setOrEmptySet[A](set: Set[A]): Set[A] = if (set == null) Set() else set

  }
}
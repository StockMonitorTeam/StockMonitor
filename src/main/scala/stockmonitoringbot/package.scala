import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext

/**
  * Created by amir.
  */
package object stockmonitoringbot {

  trait ActorSystemComponent {
    implicit val system: ActorSystem
    implicit val materializer: ActorMaterializer
  }

  trait ExecutionContextComponent {
    implicit val executionContext: ExecutionContext
  }

  trait ActorSystemComponentImpl extends ActorSystemComponent {
    override implicit lazy val system: ActorSystem = ActorSystem("ActorSystem")
    override implicit lazy val materializer: ActorMaterializer = ActorMaterializer()
  }

  trait ExecutionContextImpl extends ExecutionContextComponent {
    this: ActorSystemComponent =>
    override implicit lazy val executionContext: ExecutionContext = system.dispatcher
  }

  trait AppConfig {
    def getKey(keyPath: String): String
  }

  trait AppConfigImpl extends AppConfig {
    private lazy val config = ConfigFactory.load()

    def getKey(keyPath: String): String = config.getString(keyPath)
  }

}

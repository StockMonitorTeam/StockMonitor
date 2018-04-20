import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

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
    self: ActorSystemComponent =>
    override implicit lazy val executionContext: ExecutionContext = system.dispatcher
  }

}

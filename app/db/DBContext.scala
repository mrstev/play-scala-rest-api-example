package db

import akka.actor.ActorSystem
import io.getquill.{MysqlAsyncContext, SnakeCase}
import javax.inject.{Inject, Singleton}
import play.api.libs.concurrent.CustomExecutionContext

package object DBContext {

  /**
    * The Quill context that will run all the quoted queries.
    */
  lazy val ctx = new MysqlAsyncContext(SnakeCase, "ctx")

}

/**
  * A custom execution context is created here and passed to all Quill DB Repositories to establish that DB blocking
  * operations should be executed in a different pool of threads than Play's ExecutionContext, which is used for CPU
  * bound tasks such as rendering.
  *
  * @param actorSystem that will handle this execution context
  */
@Singleton
class QuillExecutionContext @Inject()(actorSystem: ActorSystem)
  extends CustomExecutionContext(actorSystem, "repository.dispatcher")

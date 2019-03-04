import javax.inject._
import com.google.inject.AbstractModule
import db.QuillExecutionContext
import models._
import net.codingwell.scalaguice.ScalaModule
import play.api.{Configuration, Environment}


/**
  * Binds the generalized traits to their particular production implementation.
  * These can be swapped for other implementations in testing, or new implementations
  * can be set up then swapped, as the application evolves.
  *
  * https://www.playframework.com/documentation/latest/ScalaDependencyInjection
  */
class Module(environment: Environment, configuration: Configuration)
    extends AbstractModule
    with ScalaModule {

  override def configure() = {

    bind[PartRepository].to[PartRepositoryImpl].in[Singleton]
    bind[AssemblyRepository].to[AssemblyRepositoryImpl].in[Singleton]
  }
}

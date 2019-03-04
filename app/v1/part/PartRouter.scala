package v1.part

import java.util.UUID

import javax.inject.Inject
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._

/**
  * Routes and URLs to the PartResource controller.
  */
class PartRouter @Inject()(controller: PartController) extends SimpleRouter {


  override def routes: Routes = {



    // Queries for assembly parts
    case GET(p"/assemblies/") =>
      controller.listAssemblies
    case GET(p"/assemblies/top-level/") =>
      controller.listTopLevelAssemblies
    case GET(p"/assemblies/sub-level/") =>
      controller.listSubLevelAssemblies

    // Queries for subsets of part collection
    case GET(p"/components/") =>
      controller.listComponentParts
    case GET(p"/orphan/") =>
      controller.listOrphanParts


    // CR_D for parts
    case GET(p"/") =>
      controller.index

    case POST(p"/") =>
      controller.process

    case GET(p"/$id") =>
      controller.show(id)

    case DELETE(p"/$id") =>
      controller.delete(id)


      // actions on and queries for particular parts
    case POST(p"/$id/add-parts/") =>
      controller.addParts(id)

    case POST(p"/$id/remove-parts/") =>
      controller.removeParts(id)

    case GET(p"/$id/children-parts/") =>
      controller.showWithChildren(id)

    case GET(p"/$id/all-parts/") =>
      controller.showAllChildren(id)

    case GET(p"/$id/all-parent-assemblies/") =>
      controller.showAllParents(id)




  }

}

object PartRouter {
  val prefix = "/v1/parts"

  def link(id: UUID): String = {
    import com.netaporter.uri.dsl._
    val url = prefix / id.toString
    url.toString()
  }
}
package v1.part

import javax.inject.Inject
import julienrf.json.derived
import julienrf.json.derived.NameAdapter
import play.api.Logger
import play.api.data.Form
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}


/**
  * Data structure for Part creation.
  */
case class CreatePartInputs(
  name: String,
  color: Option[String],
  material: Option[String]
)

/** Data structure for adding or removing children parts */
case class PartList(parts: List[String])

object PartList {
  /** Implicit converter between PartsList case class and JSON */
  implicit val partsListOFormat :OFormat[PartList] = derived.flat.oformat[PartList]((__ \ "type").format)
}

/**
  * Takes HTTP requests and produces JSON.
  */
class PartController @Inject()(cc: PartControllerComponents)(implicit ec: ExecutionContext)
  extends PartBaseController(cc) {

  private val logger = Logger(getClass)


  /**
    * Form validation for POSTs trying to create new parts.
    */
  private val createPartForm: Form[CreatePartInputs] = {
    import play.api.data.Forms._

    Form(
      mapping(
        "name" -> nonEmptyText,
        "color" -> optional(text),
        "material" -> optional(text)
      )(CreatePartInputs.apply)(CreatePartInputs.unapply)
    )
  }


  /**
    *
    * @return the complete list of parts in the data store
    */
  def index: Action[AnyContent] = PartAction.async { implicit request =>
    logger.trace("index: ")

    partResourceHandler.listAllParts(request.path).map { parts =>
      Ok(Json.toJson(parts))
    }
  }

  /**
    * Creates and persists new part resource, using the createPartForm to validate JSON structure and required
    * fields.
    *
    * @return the newly created part, with link in header, or BadRequest if the JSON POST body is malformed
    */
  def process: Action[AnyContent] = PartAction.async { implicit request =>

    def failure(badForm: Form[CreatePartInputs]) = {
      Future.successful(BadRequest(badForm.errorsAsJson))
    }

    def success(input: CreatePartInputs) = {
      logger.trace(s"creating from form input: $input")

      partResourceHandler.create(input).map { part =>
        Created(Json.toJson(part)).withHeaders(LOCATION -> part.link)
      }
    }

    createPartForm.bindFromRequest().fold(failure, success)
  }

  /**
    *
    * @param id the requested part
    * @return the part resource, if it exists
    */
  def show(id: String): Action[AnyContent] = PartAction.async {
    implicit request =>
      logger.trace(s"show: rootId = $id")
      partResourceHandler.lookup(id).map {
        case Some(part) => Ok(Json.toJson(part))
        case None => NotFound
      }
  }

  /**
    * Deletes a resource specified by the rootId, if it exists
    * @param id the UUID to delete
    * @return as an idempotent action, should always return NoContent
    */
  def delete(id: String): Action[AnyContent] = PartAction.async {
    implicit request =>
      logger.trace(s"deleting: rootId = $id")
      partResourceHandler.delete(id).map {
        _ => NoContent
      }
  }

  /**
    *
    * @param id the id of the part
    * @return the part resource if it exists, and children parts if any are linked.
    */
  def showWithChildren(id: String): Action[AnyContent] = PartAction.async {
    implicit request =>
      logger.trace(s"show: rootId = $id")
      partResourceHandler.lookupWithChildren(id).map {
        case Some(part) => Ok(Json.toJson(part))
        case None => NotFound
      }
  }

  /**
    * Takes a POST with a part id and a JSON Body describing the list of children part ids to be added under the part
    * Attempts to add the children to the root part. Does not error if any child parts do not exist, or already added
    * to this part.
    *
    * @param id the id of the  part
    * @return The BOM resource if it exists, with its updated list of direct child parts, or a BadRequest if the JSON
    *         body cannot conform to [[PartList]]
    */
  def addParts(id: String): Action[JsValue] = Action.async(parse.tolerantJson) {
    implicit request: Request[JsValue] =>

      logger.trace(s"Incoming JSON: ${request.body}")
      val partsOpt = request.body.asOpt[PartList]

      partsOpt.map { pList =>
        partResourceHandler.addParts(id, pList.parts).map {
          case Some(part: PartResource) => Ok(Json.toJson(part))
          case Some(part: AssemblyResource) => Ok(Json.toJson(part))
          case None => NotFound
        }
      } getOrElse Future.successful(BadRequest)
  }

  /**
    * Takes a POST with a part id and a JSON Body describing the list of children part ids to be removed under the part.
    * Attempts to remove the children from the root part. Does not error if any child parts do not exist, or do not
    * currently belong to this part.
    *
    * @param id the id of the  part
    * @return The BOM resource if it exists, with its updated list of direct child parts, or a BadRequest if the JSON
    *         body cannot conform to [[PartList]]
    */
  def removeParts(id: String): Action[JsValue] = Action.async(parse.tolerantJson) {
    implicit request: Request[JsValue] =>

      logger.trace(s"Request Body: ${request.body}")
      val partsOpt = request.body.asOpt[PartList]

      partsOpt.map { pList =>
        logger.trace(s"Parts list: $pList")
        partResourceHandler.removeParts(id, pList.parts).map {
          case Some(part: PartResource) => Ok(Json.toJson(part))
          case Some(part: AssemblyResource) => Ok(Json.toJson(part))
          case None => NotFound
        }
      } getOrElse Future.successful(BadRequest)
  }

  /**
    *
    * @param id the id of the part
    * @return the Part resources in the assembly trees below this part. Will return an empty list for a part with no
    *         children, or a NotFound if the part does not exist.
    */
  def showAllChildren(id: String): Action[AnyContent] = PartAction.async { implicit request =>
      logger.trace(s"show full part tree: rootId = $id")

      partResourceHandler.getAllChildrenParts(id, request.path).map {
        case Some(partListRes: PartListResource) => Ok(Json.toJson(partListRes))
        case None => NotFound
      }
  }

  /**
    *
    * @param id the id of the part
    * @return the Part resources in the assembly trees above this part. Will return an empty list for a part with no
    *         parents, or a NotFound if the part does not exist.
    */
  def showAllParents(id: String): Action[AnyContent] = PartAction.async { implicit request =>
      logger.trace(s"show full part tree: rootId = $id")

    partResourceHandler.getAllParentParts(id, request.path).map {
      case Some(partListRes: PartListResource) => Ok(Json.toJson(partListRes))
      case None => NotFound
    }
  }

  /**
    *
    * @return a resource listing the parts which are components of one or more assemblies
    */
  def listComponentParts = Action.async { implicit request =>
    partResourceHandler.listComponentParts(request.path).map { parts =>
      Ok(Json.toJson(parts))
    }
  }

  /**
    *
    * @return a resource listing parts which are neither assemblies nor components of an another assembly
    */
  def listOrphanParts = Action.async { implicit request =>
    partResourceHandler.listOrphanParts(request.path).map { parts =>
      Ok(Json.toJson(parts))
    }
  }

  /**
    * @return a resource listing parts which are assemblies
    */
  def listAssemblies = Action.async { implicit request =>
    partResourceHandler.listAssemblies(request.path).map { parts =>
      Ok(Json.toJson(parts))
    }
  }

  /**
    * @return a resource listing parts which are top level assemblies
    */
  def listTopLevelAssemblies = Action.async { implicit request =>
    partResourceHandler.listTopLevelAssemblies(request.path).map { parts =>
      Ok(Json.toJson(parts))
    }
  }

  /**
    * @return a resource listing parts which are sub level assemblies
    */
  def listSubLevelAssemblies = Action.async { implicit request =>
    partResourceHandler.listSubLevelAssemblies(request.path).map { parts =>
      Ok(Json.toJson(parts))
    }
  }

}

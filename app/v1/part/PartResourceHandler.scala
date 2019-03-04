package v1.part

import java.util.UUID

import db.Part
import javax.inject.Inject
import models.{AssemblyRepository, PartRepository}
import play.api.Logger

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try





/**
  * Controls access to the backend data, returning [[PartResource]]
  */
class PartResourceHandler @Inject()(
  partRepository: PartRepository,
  assemblyRepository: AssemblyRepository
)(implicit ec: ExecutionContext) {

  private val logger = Logger(this.getClass)

  /**
    *
    * @param id a string
    * @return returns Some of UUID if the formatting of the string is correct for UUID, else a None.
    */
  private def uuidFromString(id:String): Option[UUID] = Try(UUID.fromString(id)).toOption


  /**
    * Creates a new Part, initially with no children. If a valid UUID is included in this request, a new part will be
    * created using that UUID (provided no existing part with that UUID is present). Otherwise, a random UUID will be
    * assigned.
    *
    * @param partForm the description of the part fom the API request
    * @param mc
    * @return the created part resource
    */
  def create(createForm: CreatePartInputs): Future[PartResource] = {

    val data = Part(UUID.randomUUID(), createForm.name, createForm.color, createForm.material, false)

    partRepository.create(data).map { created =>
      PartResource(created)
    }
  }

  /**
    *
    * @param id the UUID of the part
    * @return  part resource if it exists, None if it does not exists
    */
  def lookup(id: String): Future[Option[BOMResource]] = {

    uuidFromString(id) map { uuid =>

      partRepository.find(uuid).map {
        _.map {
          case p: Part => PartResource(p)
         // case a: Assembly => AssemblyResource(a)
        }
      }

    } getOrElse Future.successful(None)
  }

  /**
    *
    * @param id the UUID of the part
    * @return the part resource if it exists, and children parts if any are linked.
    */
  def lookupWithChildren(id: String): Future[Option[BOMResource]] = {

    uuidFromString(id).map { uuid =>

      findWithChildrenParts(uuid)

    }getOrElse (Future.successful(None))
  }


  /**
    * Deletes the part if it exists with this rootId.
    *
    * @param id the UUID of the part
    * @return the count of parts that have been deleted (0 or 1).
    */
  def delete(id: String): Future[Long] = {
    uuidFromString(id) map { uuid =>
      partRepository.delete(uuid)
    } getOrElse {
      Future.successful(0)
    }
  }

  /**
    * Attempts to add the children to the root part. Does not error if any child parts do not exist, or already added
    * to this root part.  If the root part does not exist, will return a None.
    *
    * @param rootId the rootId of the root part
    * @param children the children ids to be added
    * @return The BOM resource if it exists, with its updated list of direct child parts
    */
  def addParts(rootId: String, children: List[String]): Future[Option[BOMResource]] = {
    val parentUUID = uuidFromString(rootId: String)
    val childrenUUIDList = children.flatMap(uuidFromString)

    parentUUID.map { uuid =>
      logger.trace(s"Adding to parent rootId $rootId the children ids $children")
      partRepository.addChildren(uuid, childrenUUIDList).flatMap { addedCnt =>
        findWithChildrenParts(uuid)
      }
    } getOrElse {
      Future.successful(None)
    }
  }

  /**
    * Attempts to remove the children to the root part. Does not error if any child parts do not exist, or not currently
    * linked to this root part.  If the root part does not exist, will return a None.
    *
    * @param rootId the id of the root part
    * @param children the children ids to be added
    * @return The BOM resource if it exists, with its updated list of direct child parts
    */
  def removeParts(rootId: String, children: List[String]): Future[Option[BOMResource]] = {

    val parentUUID = uuidFromString(rootId: String)
    val childrenUUIDList = children.flatMap(uuidFromString)

    parentUUID.map { uuid =>
      partRepository.removeChildren(uuid, childrenUUIDList).flatMap { removedCnt =>
        findWithChildrenParts(uuid)
      }
    } getOrElse {
      Future.successful(None)
    }
  }

  /**
    * @param uuid the if of the part
    * @return The BOM resource if it exists, with its current list of direct child parts
    */
  private def findWithChildrenParts(uuid:UUID): Future[Option[BOMResource]] = {
    partRepository.findChildren(uuid).flatMap { parts =>
      partRepository.find(uuid).map {
        _.map { root =>
          if(parts.nonEmpty) AssemblyResource(root, parts)
          else PartResource(root)
        }
      }
    }
  }

  /**
    *
    * @param id the UUID of the part
    * @return the Part resources in the assembly trees below this part
    */
  def getAllChildrenParts(id:String, path:String): Future[Option[PartListResource]] = {
    uuidFromString(id) map { uuid =>

      partRepository.getAllChildren(uuid).map { listOpt =>

        listOpt.map { parts =>
          PartListResource(
            path,
            parts.map(PartResource(_))
          )
        }
      }

    } getOrElse Future.successful(None)
  }

  /**
    *
    * @param id the UUID of the part
    * @param path the incoming path of the request at which this resource is located
    * @return the Part resources in which this part is a direct or indirect component
    */
  def getAllParentParts(id:String, path: String): Future[Option[PartListResource]] = {
    uuidFromString(id) map { uuid =>

      partRepository.getAllParents(uuid).map { listOpt =>

        listOpt.map { parts =>
          PartListResource(
            path,
            parts.map(PartResource(_))
          )
        }
      }
    } getOrElse Future.successful(None)
  }

  /**
    *
    * @param path the incoming path of the request at which this resource is located
    * @return a resource listing parts which are assemblies
    */
  def listAllParts(path: String): Future[PartListResource] = {
    partRepository.list().map { parts =>
      PartListResource(
        path,
        parts.map(PartResource(_))
      )
    }
  }

  /**
    *
    * @param path the incoming path of the request at which this resource is located
    * @return a resource listing the parts which are components of one or more assemblies
    */
  def listComponentParts(path: String): Future[PartListResource] = {
    assemblyRepository.listComponentParts().map { parts =>
      PartListResource(
        path,
        parts.map(PartResource(_)).toList
      )
    }
  }

  /**
    *
    * @param path the incoming path of the request at which this resource is located
    * @return a resource listing parts which are neither assemblies nor components of an another assembly
    */
  def listOrphanParts(path: String): Future[PartListResource] = {
    assemblyRepository.listOrphanParts().map { parts =>
      PartListResource(
        path,
        parts.map(PartResource(_)).toList
      )
    }
  }

  /**
    *
    * @param path the incoming path of the request at which this resource is located
    * @return a resource listing parts which are assemblies
    */
  def listAssemblies(path: String): Future[PartListResource] = {
    assemblyRepository.listAssemblies().map { parts =>
      PartListResource(
        path,
        parts.map(PartResource(_)).toList
      )
    }
  }

  /**
    *
    * @param path the incoming path of the request at which this resource is located
    * @return a resource listing parts which are top level assemblies
    */
  def listTopLevelAssemblies(path: String): Future[PartListResource] = {
    assemblyRepository.listTopLevelAssemblies().map { parts =>
      PartListResource(
        path,
        parts.map(PartResource(_)).toList
      )
    }
  }

  /**
    *
    * @param path the incoming path of the request at which this resource is located
    * @return a resource listing parts which are sub level assemblies
    */
  def listSubLevelAssemblies(path: String): Future[PartListResource] = {
    assemblyRepository.listSubAssemblies().map { parts =>
      PartListResource(
        path,
        parts.map(PartResource(_)).toList
      )
    }
  }


}

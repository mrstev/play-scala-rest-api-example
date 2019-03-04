package models

import java.util.UUID

import db.DBContext.ctx._
import db.{AssemblyPart, BOMPart, Part, QuillExecutionContext}
import javax.inject.{Inject, Singleton}
import play.api.Logger

import scala.concurrent.Future


trait PartRepository {

  /**
    *
    * @param p the part to create
    * @return the new part
    */
  def create(p: Part): Future[Part]

  /**
    *
    * @param id the UUID of the part
    * @return the part, if one exists
    */
  def find(id: UUID): Future[Option[BOMPart]]

  /**
    *
    * @return a list of all parts
    */
  def list(): Future[List[Part]]

  /**
    *
    * @param p the part to be deleted
    * @return a count of the number of parts deleted (0 or 1)
    */
  def delete(p: UUID):Future[Long]

  /**
    *
    * @param p the new state of the part
    * @return the uuid if the part that has been updated
    */
  def update(p: Part):Future[UUID]

  /**
    *
    * @param parentId the UUID of the part
    * @return the first-level children of this part, if any exist
    */
  def findChildren(parentId: UUID): Future[List[Part]]

  /**
    *
    * @param partId the UUID of the child part
    * @return the list of parents, if any, that this part has
    */
  def findParents(partId: UUID): Future[List[Part]]

  /**
    * Will attempt to add children to a parent part. If the parent does not exist, no links will be created, and no
    * errors is thrown. If a subset of children do not exist, those links will not be created, and no errors thrown.
    *
    * @param p the UUID of the part
    * @param children the list of UUIDs of the children parts
    * @return a count of the successfully added parts
    */
  def addChildren(p:UUID, children:List[UUID]):Future[Long]

  /**
    * Will attempt to remove children of a parent part. If the parent does not exist, no links will be removed, and no
    * errors is thrown. If a subset of children do not exist, those links will not be removed, and no errors thrown.
    *
    * @param p the UUID of the part
    * @param children the list of UUIDs of the children parts
    * @return a count of the successfully removed parts
    */
  def removeChildren(p:UUID, children:List[UUID]):Future[Long]

  /**
    * Provides an Optional flattened list of the children assemblies and component parts of the parent part.
    * If one or more parts are linked multiple times through sub-assemblies, these parts will appear more than once.
    *
    * If the part rootId itself does not exist, returns a Future of None.
    *
    * @param parentId
    * @return An Optional flattened List of all found children links to the part. If the part is not found, returns None
    */
  def getAllChildren(parentId: UUID): Future[Option[List[Part]]]

  /**
    * Provides an Optional flattened list of the children assemblies and component parts of the parent part.
    * If one or more parts are linked multiple times through sub-assemblies, these parts will appear more than once.
    *
    * If the part rootId itself does not exist, returns a Future of None.
    *
    * @param partId
    * @return An Optional flattened List of all found parent links to the part.  If the part is not found, returns None
    */
  def getAllParents(childPartId: UUID): Future[Option[List[Part]]]
}

/**
  * An asynchronous implementation using QuillExecutionContext for the PartRepository.
  *
  */
@Singleton
class PartRepositoryImpl @Inject()()(implicit ec: QuillExecutionContext) extends PartRepository {

  private val logger = Logger(this.getClass)

  import db.Schema._

  /** @inheritdoc */
  def find(id: UUID): Future[Option[BOMPart]] = {
    logger.trace(s"find: $id ")

    run(findPart(id)).map(_.headOption)
  }

  /** @inheritdoc */
  def list(): Future[List[Part]] = run(parts.filter(p => !p.deleted))

  /** @inheritdoc */
    def create(p: Part):Future[Part] = run(parts.insert(lift(p))).map(_ => p)

  /** @inheritdoc */
  def delete(uuid: UUID):Future[Long] = {
    run(parts.filter(_.id == lift(uuid)).delete)
  }

  /** @inheritdoc */
  def update(p: Part):Future[UUID] = {
    run(parts.filter(_.id == lift(p.id)).update(lift(p))).map(_ => p.id)
  }

  private def findPart(partId:UUID) = quote {
    parts.filter(p => p.id == lift(partId))
  }

  private def findParts(partIds:List[UUID]) = quote {
    parts.filter(p => liftQuery(partIds).contains(p.id))
  }

  private def getChildIds(parentIds: List[UUID]) = quote {
    assemblyParts.filter(ap => liftQuery(parentIds).contains(ap.assemblyId))
      .map(_.partId).distinct
  }

  private def getParentIds(childIds: List[UUID]) = quote {
    assemblyParts.filter(ap => liftQuery(childIds).contains(ap.partId))
      .map(_.assemblyId).distinct
  }

  /** @inheritdoc */
  def findChildren(parentId: UUID): Future[List[Part]] = run {
   assemblyParts.filter(ap => ap.assemblyId == lift(parentId))
      .flatMap(ap => parts.filter(p => p.id == ap.partId))
  }

  /** @inheritdoc */
  def findParents(partId: UUID): Future[List[Part]] = run {
    assemblyParts.filter(ap => ap.assemblyId == lift(partId))
      .flatMap(ap => parts.filter(p => p.id == ap.assemblyId))
  }

  /** @inheritdoc */
  def addChildren(parentId:UUID, childrenIds:List[UUID]):Future[Long] = {

    logger.trace(s"Adding to parent rootId $parentId the children ids $childrenIds")

    val assembly: Future[Option[Part]] = run(findPart(parentId)).map(_.headOption)
    val childParts: Future[List[Part]] = run(findParts(childrenIds))

    val results = for {
      assmOpt <- assembly if assmOpt.nonEmpty
      pList <- childParts
    } yield {
      logger.trace(s"parent part $assmOpt adding children $pList")

      val assmParts = pList map { part => AssemblyPart(assmOpt.get.id, part.id) }


      val inserts = assmParts.map { assmPart =>
        run(assemblyParts.insert(lift(assmPart)).onConflictIgnore)
      }
      Future.sequence(inserts)
    }
    results.flatten.map(_.sum)
  }

  /** @inheritdoc */
  def removeChildren(parentId: UUID, childrenIds: List[UUID]): Future[Long] = {

    logger.trace(s"Removing from parent rootId $parentId the children ids $childrenIds")

    val assembly: Future[Option[Part]] = run(findPart(parentId)).map(_.headOption)
    val childParts: Future[List[Part]] = run(findParts(childrenIds))

    val results = for {
      assmOpt <- assembly if assmOpt.nonEmpty
      pList <- childParts
    } yield {
      logger.trace(s"parent part $assmOpt removing children $pList")

      run(assemblyParts.filter(ap =>
        ap.assemblyId == lift(assmOpt.get.id) &&
          liftQuery(pList.map(_.id)).contains(ap.partId))
        .delete)
    }
    results.flatten
  }

  /** @inheritdoc */
  def getAllChildren(parentId: UUID): Future[Option[List[Part]]] = {

    getChildren(Nil, List(parentId)) map { partList =>

      val (child, parents) = partList.partition(_.id == parentId)

      if(child.isEmpty) None else Some(parents)
    }
  }

  private def getChildren(prevParts:List[Part], partIds: List[UUID]): Future[List[Part]] = {
    if (partIds.isEmpty)  Future.successful(prevParts)
    else {
      for {
        parts <- run(findParts(partIds))
        children <- run(getChildIds(partIds))
      } yield {
        val collection = prevParts ++ parts
        getChildren(collection, children)
      }
    }.flatten
  }

  /** @inheritdoc */
  def getAllParents(childPartId: UUID): Future[Option[List[Part]]] = {

    getParents(Nil, List(childPartId)) map { partList =>

      val (child, parents) = partList.partition(_.id == childPartId)

      if(child.isEmpty) None else Some(parents)
    }
  }

  private def getParents(prevParts:List[Part], partIds: List[UUID]): Future[List[Part]] = {
    if (partIds.isEmpty)  Future.successful(prevParts)
    else {
      for {
        parts <- run(findParts(partIds))
        parents <- run(getParentIds(partIds))
      } yield {
        val collection = prevParts ++ parts
        getParents(collection, parents)
      }
    }.flatten
  }

}
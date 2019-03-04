package models

import java.util.UUID

import db.DBContext.ctx._
import db.{BOMPart, Part, QuillExecutionContext}
import javax.inject.{Inject, Singleton}
import play.api.Logger

import scala.concurrent.Future

//case class Assembly(
//  id: UUID,
//  name: String,
//  color: Option[String],
//  material: Option[String],
//  deleted:Boolean,
//  children: List[Part]
//) extends BOMPart
//
//
//object Assembly{
//  def apply(p:Part, children:List[Part]): Assembly = {
//    Assembly(
//      id = p.id,
//      name = p.name,
//      color = p.color,
//      material = p.material,
//      deleted = p.deleted,
//      children = children
//    )
//  }
//}

trait AssemblyRepository {
  /**
    *
    * @return a unique list of assemblies, i.e., parts that have a child part.
    */
  def listAssemblies(): Future[Iterable[BOMPart]]

  /**
    *
    * @return a unique list of assemblies that are  not part of another assembly.
    */
  def listTopLevelAssemblies(): Future[Iterable[BOMPart]]

  /**
    *
    * @return a unique list of assemblies that are part of at least one other assembly.
    */
  def listSubAssemblies(): Future[Iterable[BOMPart]]

  /**
    *
    * @return a unique list of all parts with neither any parent assembly nor any child part.
    */
  def listOrphanParts(): Future[Iterable[BOMPart]]

  /**
    *
    * @return a unique list of all parts with a parent assembly but no child parts.
    */
  def listComponentParts(): Future[Iterable[BOMPart]]

}

/**
  * An asynchronous implementation using QuillExecutionContext for the PartRepository.
  *
  */
@Singleton
class AssemblyRepositoryImpl @Inject()()(implicit ec: QuillExecutionContext) extends AssemblyRepository{

  private val logger = Logger(this.getClass)

  import db.Schema._

  /**
    * A query that finds assemblies, i.e., parts that have at least one child part
    */
  private val assemblies = quote {
    for {
      p <- parts
      a <- assemblyParts.join(a => p.id == a.assemblyId).groupBy(_.assemblyId)
    } yield (p)
  }

  /** @inheritdoc */
  def listAssemblies(): Future[Iterable[BOMPart]] = {
    run(assemblies)
  }

  /** @inheritdoc */
  def listTopLevelAssemblies(): Future[Iterable[BOMPart]] = {

    val topLevel = quote {
      assemblies.leftJoin(assemblyParts)
        .on { case (p, o) => p.id == o.partId }
        .filter { case (_, o) => o.map(_.partId).isEmpty }
        .map { case (p, _) => p }
    }

    run(topLevel)
  }


  /** @inheritdoc */
  def listSubAssemblies(): Future[Iterable[Part]] = {
    val subLevel = quote {
      assemblies.leftJoin(assemblyParts)
        .on { case (p, o) => p.id == o.partId }
        .filter { case (_, o) => o.map(_.partId).nonEmpty }
        .map { case (p, _) => p }
        .distinct
    }

    run(subLevel)
  }


  /** @inheritdoc */
  def listOrphanParts(): Future[Iterable[BOMPart]] = {

    val hasNoParentNoChild = quote {

      parts.leftJoin(assemblyParts)
        .on { case (p, ap1) => p.id == ap1.partId }
        .filter { case (_, ap1) => ap1.map(_.partId).isEmpty }
        .leftJoin(assemblyParts)
        .on { case ((p, ap1), ap2) => p.id == ap2.assemblyId }
        .filter { case ((p, ap1), ap2) => ap2.map(_.partId).isEmpty }
        .map { case ((p, ap1), ap2) => p }
    }

    run(hasNoParentNoChild)

  }


  /** @inheritdoc */
  def listComponentParts(): Future[Iterable[BOMPart]] = {

    val hasSomeParentNoChild = quote {

      parts.join(assemblyParts)
        .on { case (p, ap1) => p.id == ap1.partId }
        .distinct
        .leftJoin(assemblyParts)
        .on { case ((p, ap1), ap2) => p.id == ap2.assemblyId }
        .filter { case ((p, ap1), ap2) => ap2.map(_.partId).isEmpty }
        .map { case ((p, ap1), ap2) => p }

    }

    run(hasSomeParentNoChild)
  }
}
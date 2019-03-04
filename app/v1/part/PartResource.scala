package v1.part

import db.{BOMPart, Part}
import julienrf.json.derived
import play.api.libs.json.{OWrites, __}


/**
  * Base trait for PartResource and AssemblyResource, sealed to provide compilation warnings when case matching is
  * incomplete .
  */
sealed trait BOMResource

case class PartDetails(color:Option[String], material: Option[String])

case class PartListResource(link: String, partsList: List[BOMResource])

case class PartResource(id: String, link: String, name: String, details: PartDetails)
  extends BOMResource

object PartResource {

  def apply(p: BOMPart): PartResource ={
    val details = PartDetails(p.color, p.material)
    PartResource(p.id.toString, PartRouter.link(p.id), p.name, details)
  }
}
object PartListResource{
  /** Implicit converter between PartsListResource case class and JSON */
  implicit val partListWrites: OWrites[PartListResource] = derived.flat.owrites[PartListResource]((__ \ "type").write)
}


/**
  * The canonical representation of the resource, translated from a [[db.Part]] and a List of childrent
  * @param id
  * @param link
  * @param name
  * @param details
  * @param parts
  */
case class AssemblyResource(
  id: String,
  link: String,
  name: String,
  details: PartDetails,
  parts: PartListResource
) extends BOMResource

object AssemblyResource {

  def apply(assm:BOMPart, children: List[Part]): AssemblyResource ={
    val details = PartDetails(assm.color, assm.material)
    val parts: List[BOMResource] = children.map(p => PartResource(p))


    val link = PartRouter.link(assm.id)
    val partList = PartListResource(s"$link/children-parts/", parts)

    AssemblyResource(
      assm.id.toString,
      link,
      assm.name,
      details,
      partList
    )
  }

}

/**
  * Holds a tree of Implicit writers between the case classes that make up BOMResources and their JSON
  * representations. The top level [[AssemblyResource]] and [[PartResource]] use the accompanying OWrites for
  * [[PartListResource]] and [[PartDetails]]. The [[BOMResource]] is there to allow writing from a value typed to the
  * general trait.
  */
object BOMResource {

  implicit val partDetailWrites: OWrites[PartDetails] = derived.owrites[PartDetails]()

  implicit val partWrites: OWrites[PartResource] = derived.flat.owrites[PartResource]((__ \ "type").write)

  implicit val partListWrites: OWrites[PartListResource] = derived.flat.owrites[PartListResource]((__ \ "type").write)

  implicit val assemblyWrites: OWrites[AssemblyResource] =
    derived.flat.owrites[AssemblyResource]((__ \ "type").write)

  implicit val bomResoruceWrites: OWrites[BOMResource] = derived.flat.owrites[BOMResource]((__ \ "type").write)
}

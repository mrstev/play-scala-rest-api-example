package db

import java.util.UUID

import db.DBContext.ctx._

case class Part(
  id: UUID,
  name: String,
  color: Option[String],
  material: Option[String],
  deleted:Boolean
) extends BOMPart {

  override def children: List[BOMPart] = Nil
}

case class AssemblyPart(
  assemblyId: UUID,
  partId: UUID
)


object Schema {

  val parts = quote(querySchema[Part]("part"))

  val assemblyParts = quote(querySchema[AssemblyPart]("assembly_part"))

}

package db

import java.util.UUID

/**
  * The base trait that both parts and assemblies will extend.
  */
trait BOMPart {
  def id: UUID
  def name: String
  def color: Option[String]
  def material: Option[String]
  def children: List[BOMPart]
}

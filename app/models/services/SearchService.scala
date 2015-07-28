package models.services

import scala.concurrent.Future
import models.Ref
import models.FeatureId
import scala.concurrent.ExecutionContext

/**
 * @author joao
 */
trait SearchService {
  def searchRegionByNamePart(namePart : String)(implicit ec : ExecutionContext) : Future[Seq[Ref[FeatureId]]]
  def searchCityByNamePart(regionId : Long, namePart : String)(implicit ec : ExecutionContext) : Future[Seq[Ref[FeatureId]]]
  def searchPlaceByNamePart(cityId : Long, namePart : String)(implicit ec : ExecutionContext) : Future[Seq[Ref[FeatureId]]]
}
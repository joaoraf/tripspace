package models.services.impl

import models.daos.FeatureDAO

import models.services.SearchService
import com.google.inject.Inject
import scala.concurrent.Future
import models.Ref
import models.FeatureId
import models.FT_Region
import scala.concurrent.ExecutionContext
import models.FT_City
import models.FT_POI
import utils.MyHelpers._

/**
 * @author joao
 */
class SearchServiceImpl @Inject() (featureDAO: FeatureDAO) extends SearchService {
  def searchRegionByNamePart(namePart : String)(implicit ec : ExecutionContext) : Future[Seq[Ref[FeatureId]]] = {
    featureDAO.findRefsByTypeName(FT_Region, namePart.unaccent.toLowerCase)
  }
  def searchCityByNamePart(regionId : Long, namePart : String)(implicit ec : ExecutionContext) : Future[Seq[Ref[FeatureId]]] = {
    featureDAO.findRefsByTypeNameAncestor(FT_City, namePart.unaccent.toLowerCase, regionId)
  }
  def searchPlaceByNamePart(cityId : Long, namePart : String)(implicit ec : ExecutionContext) : Future[Seq[Ref[FeatureId]]] = {
    featureDAO.findRefsByTypeNameAncestor(FT_POI, namePart.unaccent.toLowerCase, cityId)
  }
}
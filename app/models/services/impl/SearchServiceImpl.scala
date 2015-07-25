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

/**
 * @author joao
 */
class SearchServiceImpl @Inject() (featureDAO: FeatureDAO) extends SearchService {
  def searchRegionByNamePart(namePart : String)(implicit ec : ExecutionContext) : Future[Seq[Ref[FeatureId]]] = {
    featureDAO.findRefsByTypeName(FT_Region, namePart)
  }
  def searchCityByNamePart(namePart : String)(implicit ec : ExecutionContext) : Future[Seq[Ref[FeatureId]]] = {
    featureDAO.findRefsByTypeName(FT_City, namePart)
  }
  def searchPlaceByNamePart(namePart : String)(implicit ec : ExecutionContext) : Future[Seq[Ref[FeatureId]]] = {
    featureDAO.findRefsByTypeName(FT_POI, namePart)
  }
}
package models.daos

import models._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext


trait FeatureDAO {
  /*def find(featureId : FeatureId)(implicit ec : ExecutionContext) : Future[Option[Feature]]

  def exists(featureId : FeatureId)(implicit ec : ExecutionContext) : Future[Boolean]
  
  def findAll(ids : Set[FeatureId])(implicit ec : ExecutionContext) : Future[Map[FeatureId,Feature]] */
  
  def findRefsByTypeName(featureType : FeatureType, namePart : String)(implicit ec : ExecutionContext) : Future[Seq[Ref[FeatureId]]]
}

trait TripDAO {
  def findPublic()(implicit ec : ExecutionContext): Future[Option[Trip]] 
  
  def find(tripId : TripId, userId : Option[UserId] = None)(implicit ec : ExecutionContext): Future[Option[Trip]]
  
  def exists(tripId : TripId, userId : Option[UserId] = None)(implicit ec : ExecutionContext) : Future[Boolean]
  
  def findByUser(userID: UserId)(implicit ec : ExecutionContext): Future[Map[TripId,Trip]]
  
  def save(trip : Trip, userId : Option[UserId] = None)(implicit ec : ExecutionContext) : Future[Trip]

}

/*trait TripDayDAO {    
  def findByTrip(tripId : TripId)(implicit ec : ExecutionContext) : Future[Map[Int,TripDay]]
  
  def exists(regionId : TripId, dayNum : Int)(implicit ec : ExecutionContext) : Future[Boolean]
  
  def save(tripId : TripId, dayNum : Int, tripDay : TripDay)(implicit ec : ExecutionContext) : Future[TripDay]
  def save(tripId : TripId, tripDays : Map[Int,TripDay])(implicit ec : ExecutionContext) : Future[TripDay]
}

trait ActivityDAO {  
  
  def allActivities(userId : Option[UserId] = None)(implicit ec : ExecutionContext) : Future[Seq[Activity]]
  
  def findByTripDay(tripId : TripId, dayNumber : Int)(implicit ec : ExecutionContext) : Future[Option[Activity]]
  
  def save(activity : Activity)(implicit ec : ExecutionContext) : Future[Activity]

}

trait VisitDAO {
  def find(visitId : VisitId)(implicit ec : ExecutionContext) : Future[Option[Visit]]
  
  def exists(visitId : VisitId)(implicit ec : ExecutionContext) : Future[Boolean]
  
  def findByCity(cityId : CityId)(implicit ec : ExecutionContext) : Future[Seq[Visit]]
  
  def findByRegion(regionId : RegionId)(implicit ec : ExecutionContext) : Future[Seq[Visit]]
  
  def save(visit : Visit)(implicit ec : ExecutionContext) : Future[Visit]
}

trait TransportDAO {
  def find(transportId : TransportId)(implicit ec : ExecutionContext) : Future[Option[Transport]]
  
  def exists(transportId : TransportId)(implicit ec : ExecutionContext) : Future[Boolean]
  
  def findFromToCity(fromCityId : Option[CityId] = None,
                      toCityId : Option[CityId] = None)
                    (implicit ec : ExecutionContext) : Future[Seq[Transport]]
  
  def save(transport : Transport)(implicit ec : ExecutionContext) : Future[Transport]
}*/

trait TransportModalityDAO {
  def find(transportModalityId : TransportModalityId)(implicit ec : ExecutionContext) : Future[Option[TransportModality]]
  
  def exists(trasportModalityId : TransportModalityId)(implicit ec : ExecutionContext) : Future[Boolean]
  
  def save(transportModality : TransportModality)(implicit ec : ExecutionContext) : Future[TransportModality]
}
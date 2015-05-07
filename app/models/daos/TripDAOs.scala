package models.daos

import models._
import scala.concurrent.Future



trait RegionDAO {
  def find(regionId : RegionId) : Future[Option[Region]]
      
  def save(region : Region) : Future[Region]    
}

trait PlaceDAO {
  def find(placeId : PlaceId) : Future[Option[Place]]
      
  def save(place : Place) : Future[Place]
}

trait TripDAO {
  
  def find(tripId : TripId): Future[Option[Trip]]
  
  def findByUser(userID: UserId): Future[Map[TripId,Trip]]
  
  def save(trip : Trip) : Future[Trip]

}

trait TripDayDAO {    
  def findByTrip(tripId : TripId) : Future[Map[Int,TripDay]]
  
  def save(tripId : TripId, dayNum : Int, tripDay : TripDay) : Future[TripDay]
  def save(tripId : TripId, tripDays : Map[Int,TripDay]) : Future[TripDay]
}

trait ActivityDAO {
  def allActivities(userId : Option[UserId] = None) : Future[Seq[Activity]]
  
  def findByTripDay(tripId : TripId, dayNumber : Int) : Future[Option[Activity]]
  
  def save(activity : Activity) : Future[Activity]

}

trait VisitDAO {
  def find(visitId : VisitId) : Future[Option[Visit]]
  
  def findByPlace(placeId : PlaceId) : Future[Seq[Visit]]
  
  def findByRegion(regionId : RegionId) : Future[Seq[Visit]]
  
  def save(visit : Visit) : Future[Visit]
}

trait TransportDAO {
  def find(transportId : TransportId) : Future[Option[Transport]]
  
  def findFromToPlace(fromPlaceId : Option[PlaceId] = None,
                      toPlaceId : Option[PlaceId] = None) : Future[Seq[Transport]]
  
  def save(transport : Transport) : Future[Transport]
}

trait TransportModalityDAO {
  def find(transportModalityId : TransportModalityId) : Future[Option[TransportModality]]
  
  def save(transportModality : TransportModality) : Future[TransportModality]
}
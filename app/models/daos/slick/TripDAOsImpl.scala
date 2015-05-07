package models.daos.slick

import play.api.db.slick._
import scala.concurrent.Future
import models.daos.slick.DBTableDefinitions._
import play.api.db.slick.Config.driver.simple._
import models._
import models.daos._
import play.api.Play.current
import scala.language.implicitConversions


import scala.concurrent.ExecutionContext

import java.util.UUID

object Converters {
  implicit def dbRegionToRegion(dbRegion : DBRegion) : Region = 
    Region(dbRegion.regionId, dbRegion.regionName,
           dbRegion.regionDescription, dbRegion.regionThumbnail)
           
  implicit def regionToDbRegion(region : Region) : DBRegion = 
    DBRegion(region.regionId, region.regionName,
           region.regionDescription, region.regionThumbnail)
           
  implicit def dbPlaceToPlace(dbPlace : DBPlace) : Place =
    Place(dbPlace.placeId,dbPlace.placeName,dbPlace.placeDescription)
  
  implicit def placeToDBPlace(place : Place) : DBPlace = 
    DBPlace(place.placeId,place.placeName,place.placeDescription)
    
  implicit def dbTranspModToDomain(t : DBTransportModality) =
      TransportModality(UUID.fromString(t.transportModalityId),t.transportModalityName)
      
  implicit def domainTranspModToDb(t : TransportModality) =
      DBTransportModality(t.transportModalityId.toString,t.transportModalityName)
      
  implicit def domainTranspToDb(t : Transport) : DBTransport = 
    DBTransport(t.transportId.toString,t.fromPlace.placeId,
                t.toPlace.placeId,t.transportModality.transportModalityId.toString,
                t.transportDescription,t.lengthHours)
  
  implicit def domainTranspToDb(t : Visit) : DBVisit = 
    DBVisit(t.visitId.toString,t.place.placeId,                
                t.visitDescription,t.lengthHours)                
}

import Converters._

object DAOUtils {
  def find_[T,R](f : Session => T => R)(p : T) : Future[R] = Future.successful {
    DB withSession { s => f(s)(p) } }
}

import DAOUtils._


class RegionDAOSlickImpl extends RegionDAO {     
  
  def findBaseRegion_(regionId : RegionId)(implicit session : Session) : Option[Region] = {
    slickRegions.filter(r => r.regionId === regionId).firstOption.
          map(x => x) 
  }
  def findBaseRegion(regionId : RegionId) : Future[Option[Region]] =  Future.successful {
    DB withSession { implicit s => findBaseRegion_(regionId) } } 
          
  def findSuperRegions(regionId : RegionId)(implicit session : Session) : Set[RegionId] =    
          slickRegionSubRegions
              .filter(r => r.subRegionId === regionId)
              .map(_.superRegionId).buildColl[Set]     
  
  
  override def find(regionId : RegionId) = Future.successful { DB.withSession { implicit s =>
      findBaseRegion_(regionId) map {
        baseRegion =>
            val superRegionIds = findSuperRegions(regionId)
            baseRegion.copy(superRegionIds = superRegionIds)
      }
    }
  }
      
  override def save(region : Region) : Future[Region] = Future.successful { DB.withSession { implicit session =>    
      val subRegionIds = slickRegionSubRegions
        .filter(r => r.superRegionId === region.regionId)
        .map(_.subRegionId)
        .buildColl[Seq]
      
      if(slickRegions
          .filter(r => r.regionId === region.regionId)
          .update(region) == 0) {                        
        slickRegions.insert(region)
      }
      
      slickRegionSubRegions.filter(_.subRegionId === region.regionId).delete
      
      slickRegionSubRegions.insertAll(
          subRegionIds.map(DBRegionSubRegion(region.regionId,_)) : _*
          )
      region
    }     
  }
}




class PlaceDAOSlickImpl extends PlaceDAO {
  def find_(placeId : PlaceId)(implicit session : Session) = {
    val basePlace = slickPlaces.filter(p => p.placeId === placeId).firstOption.
            map(x => x : Place)
       
    basePlace.map {           
       _.copy(placeRegionIds = slickPlaceRegions.filter(_.placeId === placeId).map(_.regionId).buildColl[Set])       
    }
  } 
  
  override def find(placeId : PlaceId) = 
    Future.successful{ DB withSession { implicit session => find_(placeId) } }
      
  override def save(place : Place) : Future[Place] = Future.successful{ DB withSession { implicit session =>
      if(slickPlaces.filter(_.placeId === place.placeId).update(place) == 0) {
        slickPlaces.insert(place)
      }
      
      slickPlaceRegions.filter(_.placeId === place.placeId).delete            
      
      slickPlaceRegions.insertAll(
            place.placeRegionIds.toSeq.map(DBPlaceRegion(place.placeId,_)) : _*
          )
      place      
    }
  }
}

class TransportModalityDAOSlickImpl extends TransportModalityDAO {
  def find_(transportModalityId : TransportModalityId)(implicit session : Session) :
        Option[TransportModality] =
    transportModalities.filter(_.transportModalityId === transportModalityId.toString)
                         .firstOption
                         .map(x => x)
  override def find(transportModalityId : TransportModalityId) = 
    Future.successful{ DB withSession { find_(transportModalityId)(_) } }      
  
  
  def save(transportModality : TransportModality) = Future.successful{ DB withSession { implicit session =>
      if(transportModalities
            .filter(_.transportModalityId === transportModality.transportModalityId.toString)
            .update(transportModality) == 0) {
         transportModalities.insert(transportModality)  
      }      
      transportModality
    }
  }
}

class TransportDAOSlickImpl extends TransportDAO {
  private val placeDAO = new PlaceDAOSlickImpl
  private val tmDAO = new TransportModalityDAOSlickImpl
  
  private def fillTransport(dbt : DBTransport)(implicit session : Session) : Option[Transport] = {
    for {
      fromPlace <- placeDAO.find_(dbt.fromPlaceId)
      toPlace <- placeDAO.find_(dbt.toPlaceId)
      tm <- tmDAO.find_(UUID.fromString(dbt.transportModalityId))           
    } yield (Transport(UUID.fromString(dbt.transportId), fromPlace, toPlace, tm,dbt.lengthHours))     
  } 
  
  override def find(transportId : TransportId) : Future[Option[Transport]]= 
    Future.successful{ DB withSession { implicit session => 
        for {
          dbt <- slickTransports
                  .filter(_.transportId === transportId.toString)
                  .firstOption
          transp <- fillTransport(dbt)           
        } yield (transp)       
    }
  }
  
  override def findFromToPlace(fromPlaceId : Option[PlaceId] = None,
                      toPlaceId : Option[PlaceId] = None) = Future.successful{ DB withSession { implicit session =>       
       val q1 = slickTransports
       val q2 = fromPlaceId.map(pId => q1.filter(_.fromPlaceId === pId)).getOrElse(q1)
       val q3 = toPlaceId.map(pId => q2.filter(_.toPlaceId === pId)).getOrElse(q2)
       q3.buildColl[Seq].map(fillTransport).flatten
    }
  }
  
  def save(transport : Transport) : Future[Transport] = Future.successful{ DB withSession { implicit session =>
      if(slickTransports.filter(_.transportId === transport.transportId.toString)
                .update(transport) == 0) {
        slickTransports.insert(transport)
      }
      transport      
    }
  }
}

class VisitDAOSlickImpl extends VisitDAO {
  private val placeDAO = new PlaceDAOSlickImpl
    
  private def fillVisit(dbv : DBVisit)(implicit session : Session) : Option[Visit] = {
    for {
      place <- placeDAO.find_(dbv.visitPlaceId)                
    } yield (Visit(UUID.fromString(dbv.visitId), place, dbv.visitDescription, dbv.lengthHours))     
  } 
  
  override def find(visitId : VisitId) : Future[Option[Visit]]= 
    Future.successful{ DB withSession { implicit session => 
          for {
            dbv <- slickVisits
                    .filter(_.visitId === visitId.toString)
                    .firstOption
            visit <- fillVisit(dbv)           
          } yield (visit)       
      }
    }
  
  override def findByPlace(placeId : PlaceId) = Future.successful{ DB withSession { implicit session =>       
       slickVisits.filter(_.visitPlaceId === placeId.toString)
         .buildColl[Seq]
         .flatMap(fillVisit)      
    }
  }
  
  override def findByRegion(regionId : RegionId) : Future[Seq[Visit]] = Future.successful{ DB withSession { implicit session =>       
      val q1 = for {
        place  <- slickPlaceRegions
        if place.regionId === regionId
        visit <- slickVisits
        if visit.visitPlaceId === place.placeId
      } yield (visit)
      q1.buildColl[Seq].flatMap(fillVisit)
    }
  }
  
  def save(visit : Visit) : Future[Visit] = Future.successful{ DB withSession { implicit session =>
      if(slickVisits.filter(_.visitId === visit.visitId.toString)
                .update(visit) == 0) {
        slickVisits.insert(visit)
      }
      visit      
    }
  }
  
  
    
}

/*
class ActivityDAOSlickImpl extends ActivityDAO {
  
  private def allPublicActivities[T <: DBAbstractActivity, TTable <: DerivedActivities[T] with Table[T]]
    (table : TableQuery[TTable])(implicit session : Session) : Seq[T] = {
    val q1 = for {
      trip <- slickTrips
      if trip.tripIsPublic
      day <- slickTripDays
      if day.tripId === trip.tripId
      activity <- table
      if activity.tripId === day.tripIp && activity.dayNumber == day.dayNumber            
    } yield (activity)
    ???
  }
  
  def allActivities(userId : Option[UserId] = None) : Future[Seq[Activity]] = 
    ???  
      
  }
  
  def findByTripDay(tripId : TripId, dayNumber : Int) : Future[Option[Activity]]
  
  def save(activity : Activity) : Future[Activity]

}
*/

/*
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


*/
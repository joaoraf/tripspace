package models.daos.slick

import play.api.db.slick.Config.driver.simple._

import ForeignKeyAction._

trait TripspaceDBTableDefinitions {
    self : SilhoutteDBTableDefinitions =>
  
    case class DBUserTrip(
      userId : String,
      tripId : String
      )
  
    case class DBTrip(
        tripId : String,        
        tripName : String,
        tripIsPublic : Boolean
        )
                   
    case class DBTripToRegion(
        tripId : String,
        regionId : String
        )
                    
    case class DBTripDay (        
        tripId : String,                
        dayNumber : Int,
        label : Option[String] = None    
        )
               
     
    
    
    case class DBActivity(
        tripId : String,
        dayNumer : Int,
        order : Int) 
        
    case class DBVisitActivity(
        tripId : String,
        dayNumber : Int,
        order : Int,
        visitId : String
        ) 
    
    case class DBTransportActivity(
        tripId : String,
        dayNumber : Int,
        order : Int,
        transportId : String
        )    
        
    case class DBVisit(
        visitId : String,
        visitPlaceId : String,
        visitDescription : String,
        lengthHours : Int
        )
    
    case class DBTransport(
        transportId : String,
        fromPlaceId : String,
        toPlaceId : String,
        transportModalityId : String,
        transportDescription : String,
        lengthHours : Int
        )    
        
    case class DBTransportModality(
        transportModalityId : String,
        transportModalityName : String
        )

    case class DBPlace(
        placeId : String,
        placeName : String,
        placeDescription : String
        )
    
    case class DBPlaceRegion(
        placeId : String,
        regionId : String
        )
  
    case class DBRegion(
        regionId : String,
        regionName : String,
        regionDescription : String,
        regionThumbnail : Option[String]
        )
        
    case class DBRegionSubRegion(
        superRegionId : String,
        subRegionId : String
        )
  
            
    class Trips(tag : Tag) extends Table[DBTrip](tag,"trip") {
      def tripId = column[String]("trip_id", O.PrimaryKey)
      def tripName = column[String]("trip_name", O.Default(""))
      def tripIsPublic = column[Boolean]("trip_is_public", O.Default(false))
      def * = (tripId,tripName,tripIsPublic) <> (DBTrip.tupled,DBTrip.unapply)
    }
    
    val slickTrips = TableQuery[Trips]
    
    class UserTrips(tag : Tag) extends Table[DBUserTrip](tag,"user_trip") {
      def userId = column[String]("user_id")
      def tripId = column[String]("trip_id")
      
      primaryKey("pk_user_trips", (userId,tripId))
      foreignKey("fk_user_trips_user", userId, self.slickUsers)(_.id, Cascade,Cascade)
      foreignKey("fk_user_trips_trip", tripId, self.slickTrips)(_.tripId, Cascade,Cascade)
      
      def * = (userId,tripId) <> (DBUserTrip.tupled, DBUserTrip.unapply)
    }
    
    val slickUserTrips = TableQuery[UserTrips]
    
    class Regions(tag : Tag) extends Table[DBRegion](tag, "region") {
      def regionId = column[String]("region_id", O.PrimaryKey)
      def regionName = column[String]("region_name", O.Default(""))
      def regionDescription = column[String]("region_description", O.Default(""))
      def regionThumbnail = column[Option[String]]("region_thumbnail")
      
      def * = (regionId, regionName, regionDescription, regionThumbnail) <> (DBRegion.tupled, DBRegion.unapply)
    }    
    
    val slickRegions = TableQuery[Regions]
    
    class RegionSubRegions(tag : Tag) extends Table[DBRegionSubRegion](tag, "region_sub_region") {
      def superRegionId = column[String]("super_region_id")
      def subRegionId = column[String]("sub_region_id")
      
      primaryKey("pk_region_sub_region", (superRegionId,subRegionId))
      foreignKey("fk_region_sub_region_super", superRegionId, self.slickRegions)(_.regionId, Cascade,Cascade)
      foreignKey("fk_region_sub_region_sub", subRegionId, self.slickRegions)(_.regionId, Cascade,Cascade)
      
      def * = (superRegionId, subRegionId) <> (DBRegionSubRegion.tupled,DBRegionSubRegion.unapply)
    }
    
    val slickRegionSubRegions = TableQuery[RegionSubRegions]
    
    class TripToRegions(tag : Tag) extends Table[DBTripToRegion](tag, "trip_to_region") {
      def tripId = column[String]("trip_id")
      def regionId = column[String]("region_id")
      
      primaryKey("pk_trip_regions", (tripId,regionId))      
      foreignKey("fk_user_trips_trip", tripId, self.slickTrips)(_.tripId, Cascade,Cascade)
      foreignKey("fk_user_trips_region", tripId, self.slickRegions)(_.regionId, Cascade,Cascade)
      
      def * = (tripId,regionId) <> (DBTripToRegion.tupled, DBTripToRegion.unapply)
    }
  
    val slickTripToRegions = TableQuery[TripToRegions]
        
    
    class TripDays(tag : Tag) extends Table[DBTripDay](tag, "trip_day") {      
      def tripId = column[String]("trip_id")
      def dayNumber = column[Int]("day_number")
      def label = column[Option[String]]("day_label")
      
      primaryKey("fk_day", (tripId, dayNumber))
      foreignKey("fk_trip_days_trip", tripId, self.slickTrips)(_.tripId, Cascade,Cascade)
      index("fk_trip_days_unique_day_number", (tripId,dayNumber), true)
      
      def * = (tripId, dayNumber, label) <> (DBTripDay.tupled, DBTripDay.unapply)
    }
    
    val slickTripDays = TableQuery[TripDays]
    
    class Activities(tag : Tag) extends Table[DBActivity](tag, "activity") {
      def tripId = column[String]("trip_id")
      def dayNumber = column[Int]("day_number")
      def order = column[Int]("activity_order")
      
      foreignKey("fk_activity_trip_day", (tripId,dayNumber), self.slickTripDays)(x => (x.tripId, x.dayNumber), Cascade,Cascade)
      primaryKey("pk_activity", (tripId,dayNumber,order))
      
      def * = (tripId,dayNumber,order) <> (DBActivity.tupled, DBActivity.unapply)
    }
        
    val slickActivities = TableQuery[Activities]
        
    
    class VisitActivity(tag : Tag) extends Table[DBVisitActivity](tag, "visit_activity") {
      def tripId = column[String]("trip_id")
      def dayNumber = column[Int]("day_number")
      def order = column[Int]("activity_order")
      def visitId = column[String]("visit_id")
      
      foreignKey("fk_visit_activity_day", (tripId,dayNumber,order), slickActivities)(x => (x.tripId, x.dayNumber, x.order), Cascade,Cascade)      
      
      primaryKey("pk_visit_activity", (tripId,dayNumber,order))
      
      foreignKey("fk_visit_activity_visit", visitId, self.slickVisits)(_.visitId, Cascade,Restrict)      
      
      def * = (tripId,dayNumber,order,visitId) <> (DBVisitActivity.tupled, DBVisitActivity.unapply)
    }
    
    val slickVisitActivities = TableQuery[VisitActivity]
    
    class TransportActivity(tag : Tag) extends Table[DBTransportActivity](tag, "transport_activity") {
      def tripId = column[String]("trip_id")
      def dayNumber = column[Int]("day_number")
      def order = column[Int]("activity_order")
      
      def transportId = column[String]("transport_id")
      
      foreignKey("fk_visit_activity_day", (tripId,dayNumber,order), slickActivities)(x => (x.tripId, x.dayNumber, x.order), Cascade,Cascade)      
      
      primaryKey("pk_visit_activity", (tripId,dayNumber,order))
            
      foreignKey("fk_transport_activity_transport", transportId, self.slickTransports)(_.transportId, Cascade,Restrict)
      
      def * = (tripId,dayNumber,order,transportId) <> (DBTransportActivity.tupled, DBTransportActivity.unapply)
    }
    
    val slickTransportActivities = TableQuery[TransportActivity]
    
    class Visits(tag : Tag) extends Table[DBVisit](tag,"visit") {
      def visitId = column[String]("visitId", O.PrimaryKey)
      def visitPlaceId = column[String]("visitPlaceId")
      def visitDescription = column[String]("visit_description", O.Default(""))
      def lengthHours = column[Int]("length_hours")
      
      foreignKey("fk_visit_place", visitPlaceId, slickPlaces)(_.placeId,Cascade,Restrict)
      
      def * = (visitId, visitPlaceId, visitDescription, lengthHours) <>
                (DBVisit.tupled,DBVisit.unapply)
    }
    
    val slickVisits = TableQuery[Visits]
            
    class Transports(tag : Tag) extends Table[DBTransport](tag,"transport") {
      def transportId = column[String]("transportId", O.PrimaryKey)
      def fromPlaceId = column[String]("fromPlaceId")
      def toPlaceId = column[String]("toPlaceId")
      def transportModalityId = column[String]("transport_modality_id")
      def transportDescription = column[String]("transport_description", O.Default(""))
      def lengthHours = column[Int]("length_hours")
      
      foreignKey("fk_transport_from_place", fromPlaceId, slickPlaces)(_.placeId,Cascade,Restrict)
      foreignKey("fk_transport_to_place", toPlaceId, slickPlaces)(_.placeId,Cascade,Restrict)
      foreignKey("fk_transport_modality", transportModalityId, transportModalities)(_.transportModalityId,Cascade,Restrict)
      
      def * = (transportId, fromPlaceId, toPlaceId, transportModalityId, transportDescription, lengthHours) <>
                (DBTransport.tupled,DBTransport.unapply)
    }
    
    val slickTransports = TableQuery[Transports]
    
    class Places(tag : Tag) extends Table[DBPlace](tag, "place") {
      def placeId = column[String]("placeId", O.PrimaryKey)
      def placeName = column[String]("placeName")
      def placeDescription = column[String]("placeDescription", O.Default(""))
      
      def * = (placeId,placeName,placeDescription) <> (DBPlace.tupled,DBPlace.unapply)
    }
    
    val slickPlaces = TableQuery[Places]
    
    class PlaceRegions(tag : Tag) extends Table[DBPlaceRegion](tag,"place_region") {
      def placeId = column[String]("placeId")
      def regionId = column[String]("region_id")
            
      primaryKey("pk_place_regions", (placeId,regionId))      
      foreignKey("fk_user_places_place", placeId, self.slickPlaces)(_.placeId, Cascade,Cascade)
      foreignKey("fk_user_places_region", placeId, self.slickRegions)(_.regionId, Cascade,Cascade)
      
      def * = (placeId,regionId) <> (DBPlaceRegion.tupled, DBPlaceRegion.unapply)
    }
    
    val slickPlaceRegions = TableQuery[PlaceRegions]
    
    class TransportModalities(tag :Tag) extends Table[DBTransportModality](tag, "transport_modality") {
      def transportModalityId = column[String]("transport_modality_id",O.PrimaryKey)
      def transportModalityName = column[String]("transport_modality_name")
      
      def * = (transportModalityId, transportModalityName) <> (DBTransportModality.tupled, DBTransportModality.unapply)
    }
    
    val transportModalities = TableQuery[TransportModalities]
}
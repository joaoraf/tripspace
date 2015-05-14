package models.daos.slickdaos

import slick.driver.PostgresDriver.api._


import ForeignKeyAction._

trait TripspaceDBTableDefinitions {
    self : SilhoutteDBTableDefinitions =>
    import models.daos.slickdaos.dbobjs._  
      
            
    class Trips(tag : Tag) extends Table[DBTrip](tag,"trip") {
      def tripId = column[String]("trip_id", O.PrimaryKey)
      def tripName = column[String]("trip_name", O.Default(""))
      def tripIsPublic = column[Boolean]("trip_is_public", O.Default(false))      
      def userId = column[String]("user_id")
      def regionId = column[String]("region_id")
      
      foreignKey("fk_trip_user", userId, self.slickUsers)(_.id, Cascade,Restrict)
      foreignKey("fk_trip_region", tripId, self.slickRegions)(_.regionId, Cascade,Restrict)
      
      def * = (tripId,tripName,tripIsPublic,userId,regionId) <> (DBTrip.tupled,DBTrip.unapply)
    }
    
    val slickTrips = TableQuery[Trips]
        
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
    
    abstract sealed class BaseActivityTable[T <: DBBaseActivity](tag : Tag, name : String) extends Table[T](tag,name) {
      def tripId : Rep[String]
      def dayNumber : Rep[Int]
      def order : Rep[Int]
    }
    
    type BaseActivityTableQuery[T <: BaseActivityTable[_ <: DBBaseActivity]] = TableQuery[T]
    
    class Activities(tag : Tag) extends BaseActivityTable[DBActivity](tag, "activity") {
      def tripId = column[String]("trip_id")
      def dayNumber = column[Int]("day_number")
      def order = column[Int]("activity_order")
      def lengthHours = column[Int]("length_hours")
      
      foreignKey("fk_activity_trip_day", (tripId,dayNumber), self.slickTripDays)(x => (x.tripId, x.dayNumber), Cascade,Cascade)
      primaryKey("pk_activity", (tripId,dayNumber,order))
      
      def * = (tripId,dayNumber,order,lengthHours) <> (DBActivity.tupled, DBActivity.unapply)
    }
        
    val slickActivities : BaseActivityTableQuery[Activities] = TableQuery[Activities] 
    
    class VisitActivity(tag : Tag) extends BaseActivityTable[DBVisitActivity](tag, "visit_activity") {
      def tripId = column[String]("trip_id")
      def dayNumber = column[Int]("day_number")
      def order = column[Int]("activity_order")
      def visitPlaceId = column[String]("visitPlaceId")
      def visitDescription = column[String]("visit_description", O.Default(""))      
      
      foreignKey("fk_visit_activity_day", (tripId,dayNumber,order), slickActivities)(x => (x.tripId, x.dayNumber, x.order), Cascade,Cascade)
      foreignKey("fk_visit_activity_place", visitPlaceId, slickPlaces)(_.placeId,Cascade,Restrict)
      
      primaryKey("pk_visit_activity", (tripId,dayNumber,order))                  
      
      def * = (tripId,dayNumber,order,visitPlaceId,visitDescription) <> (DBVisitActivity.tupled, DBVisitActivity.unapply)
    }
    
    val slickVisitActivities : BaseActivityTableQuery[VisitActivity] = TableQuery[VisitActivity]
    
    class TransportActivity(tag : Tag) extends BaseActivityTable[DBTransportActivity](tag, "transport_activity") {
      def tripId = column[String]("trip_id")
      def dayNumber = column[Int]("day_number")
      def order = column[Int]("activity_order")
      
      def fromPlaceId = column[String]("fromPlaceId")
      def toPlaceId = column[String]("toPlaceId")
      def transportModalityId = column[String]("transport_modality_id")
      def transportDescription = column[String]("transport_description", O.Default(""))
      foreignKey("fk_transport_activity_from_place", fromPlaceId, slickPlaces)(_.placeId,Cascade,Restrict)
      foreignKey("fk_transport_activity_to_place", toPlaceId, slickPlaces)(_.placeId,Cascade,Restrict)
      foreignKey("fk_transport_activity_modality", transportModalityId, transportModalities)(_.transportModalityId,Cascade,Restrict)
            
      foreignKey("fk_transport_activity_day", (tripId,dayNumber,order), slickActivities)(x => (x.tripId, x.dayNumber, x.order), Cascade,Cascade)      
      
      primaryKey("pk_transport_activity", (tripId,dayNumber,order))          
      
      def * = (tripId,dayNumber,order,fromPlaceId,toPlaceId,transportModalityId,transportDescription) <> (DBTransportActivity.tupled, DBTransportActivity.unapply)
    }
    
    val slickTransportActivities : BaseActivityTableQuery[TransportActivity] = TableQuery[TransportActivity]            
    
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
    
    
    val tripTables = Seq(
        transportModalities, slickRegions, slickPlaces, slickPlaceRegions, 
        slickTrips, slickTripDays, slickActivities, slickVisitActivities, 
        slickTransportActivities, slickRegionSubRegions)
}
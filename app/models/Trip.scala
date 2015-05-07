package models

import scala.collection.immutable.SortedMap

/**
 * The Trip object
 */
case class Trip(
    tripId : TripId = java.util.UUID.randomUUID(),
    userId : User,
    tripName : String = "",
    tripIsPublic : Boolean = false,
    days : SortedMap[Int,TripDay],
    region : Region    
    ) 
    
case class Region(
    regionId : RegionId,
    regionName : String,
    regionDescription : String = "",
    regionThumbnail : Option[String] = None,
    superRegionIds : Set[RegionId] = Set()
    )

    
case class TripDay (       
    label : Option[String],
    activities : Seq[Activity]
    )

sealed trait Activity {
  val lengthHours : Int
}
        
case class Visit(
    visitId : VisitId,
    place : Place,
    visitDescription : String,
    lengthHours : Int
    ) extends Activity

case class Transport(
    transportId : TransportId,
    fromPlace : Place,
    toPlace : Place,
    transportModality : TransportModality,
    lengthHours : Int
    ) extends Activity    
    
case class TransportModality(
    transportModalityId : TransportModalityId,
    transportModalityName : String
    )

case class Place(
    placeId : PlaceId,
    placeName : String,
    placeDescription : String,
    placeRegionIds : Set[RegionId] = Set()
    )    
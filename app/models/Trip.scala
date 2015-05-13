package models

import scala.collection.SortedMap

/**
 * The Trip object
 */
case class Trip(
    tripId : TripId = java.util.UUID.randomUUID(),
    user : User,
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
    superRegions : Set[Region] = Set()
    )

    
case class TripDay (       
    label : Option[String],
    activities : Seq[Activity]
    )

sealed trait Activity {
  val lengthHours : Int
}

case class UndefinedActivity(lengthHours : Int) extends Activity

case class Visit(    
    place : Place,
    visitDescription : String,
    lengthHours : Int
    ) extends Activity

case class Transport(    
    fromPlace : Place,
    toPlace : Place,
    transportModality : TransportModality,
    description : String,
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
    regions : Set[Region] = Set()
    )    
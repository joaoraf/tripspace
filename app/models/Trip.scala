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
    city : City,
    visitDescription : String,
    lengthHours : Int
    ) extends Activity

case class Transport(    
    fromCity : City,
    toCity : City,
    transportModality : TransportModality,
    description : String,
    lengthHours : Int
    ) extends Activity    
    
case class TransportModality(
    transportModalityId : TransportModalityId,
    transportModalityName : String
    )

case class City(
    cityId : CityId,
    cityName : String,
    cityDescription : String,
    pois : Set[POI] = Set(),
    regions : Set[Region] = Set()
    )    

case class POI(
    id : POIId,
    name : String,
    description : String)    
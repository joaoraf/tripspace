package models

import scala.collection.SortedMap

case class Ref[T](id : T, name : String = "")

object Ref {
  val defaultUUID_Ref = Ref(java.util.UUID.randomUUID())
}

case class Trip(
    ref : Ref[TripId] = Ref.defaultUUID_Ref,
    isPublic : Boolean = false,
    userRef : Ref[UserId],    
    days : SortedMap[Int,TripDay],
    regionRef : Ref[RegionId])

case class Region(
    ref : Ref[RegionId],
    description : String = "",
    thumbnail : Option[String] = None,
    optSuperRegionRef : Option[Ref[RegionId]] = None,
    setSubRegionRefs : Set[Ref[RegionId]] = Set(),
    setCityRefs : Set[Ref[CityId]] = Set()
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
    poiRef : Ref[POIId],
    description : String,
    lengthHours : Int
    ) extends Activity

case class Transport(    
    fromCity : Ref[CityId],
    toCity : Ref[CityId],
    transportModalityRef : Ref[TransportModalityId],
    description : String,
    lengthHours : Int
    ) extends Activity    
    
case class TransportModality(
    ref : Ref[TransportModalityId]
    )

case class City(
    ref : Ref[CityId],
    description : String,
    regionRef : Ref[RegionId],
    poiRefs : Set[Ref[POIId]] = Set()
    ) 

case class POI(
    ref : Ref[POIId],    
    description : String,
    cityRef : Ref[CityId])    
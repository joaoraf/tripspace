package forms

import play.api._
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import models.Trip
import models.Ref
import java.util.UUID
import models.UserId

case class TripBaseData(
    name : String,
    description : String,
    regionId : Long,
    regionName : String,
    cityId : Long,
    cityName : String
    ) {
  def toTrip(userId : UserId, tripId : UUID = UUID.randomUUID()) = 
      Trip(
          ref = Ref(tripId,name),          
          userRef = Ref(userId,""),    
          description = description,
          regionRef = Ref(regionId,""),
          cityRef = Ref(cityId,"")
          )
  
}

/**
 * @author joao
 */
object TripBaseData {
  def mapping(prefix : String = "") = Forms.mapping(      
      "name" -> of[String],
      "description" -> of[String],
      "regionId" -> of[Long],
      "regionName" -> of [String],
      "cityId" -> of[Long],
      "cityName" -> of [String]
      )(TripBaseData.apply)(TripBaseData.unapply).
      withPrefix(prefix)
    
  def form(prefix : String = "") = Form(mapping(prefix))  
  def fromTrip(trip : Trip) =
    TripBaseData(trip.ref.name,trip.description,trip.regionRef.id,trip.regionRef.name,trip.cityRef.id,trip.cityRef.name)
}
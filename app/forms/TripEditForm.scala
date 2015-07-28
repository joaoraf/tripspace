package forms

import models.POIId
import models.CityId
import models.TransportModalityId
import play.api.data.Forms.{mapping => _mapping,_}
import play.api.data.format._
import play.api.data.format.Formats._
import play.api.data._
import models.Trip
import models.TripDay
import models.Activity
import models.UndefinedActivity
import models.Visit
import models.Transport
import models.TripId
import models.UserId
import models.Ref
import scala.collection.SortedMap
import models.UndefinedActivity
import models.FeatureId

case class ActivityData(
  typ : Int,
  order : Int,
  lengthHours : Int,
  description : String = "",
  poiID : Option[FeatureId] = None,  
  toCityId : Option[FeatureId] = None,
  modalityId : Option[TransportModalityId] = None
  ) {
  import ActivityData._
  lazy val toActivity = typ match {
    case TYP_FREE => UndefinedActivity(lengthHours)
    case TYP_VISIT => Visit(Ref(poiID.get), description, lengthHours)
    case TYP_TRANSPORT => 
      Transport(        
        toCity = Ref(toCityId.get),
        transportModalityRef = Ref(modalityId.get),
        description,
        lengthHours)
  }
}

object ActivityData {
  val TYP_FREE = 0
  val TYP_VISIT = 1
  val TYP_TRANSPORT = 2
  
  def mapping(prefix : String = "") = _mapping(
      "typ" -> of[Int],
      "order" -> of[Int],
      "lengthHours" -> of[Int],
      "description" -> of[String],
      "poiID" -> optional(of[Long]),      
      "toCityId" -> optional(of[Long]),
      "modalityId" -> optional(of[String])
      )(ActivityData.apply)(ActivityData.unapply).withPrefix(prefix)
  def form(prefix : String = "") = Form(mapping(prefix))
  def fromActivity(order : Int, activity : Activity) : ActivityData = activity match {    
    case u : UndefinedActivity => ActivityData(TYP_FREE,order,u.lengthHours)
    case v : Visit => ActivityData(TYP_VISIT,order,v.lengthHours,v.description,poiID = Some(v.poiRef.id))
    case t : Transport => ActivityData(TYP_TRANSPORT,order,t.lengthHours,t.description,
                                       toCityId = Some(t.toCity.id),
                                       modalityId = Some(t.transportModalityRef.id))
  }
}
    
case class TripDayData(
    dayNumber : Int,
    label : String = "",
    activities : Seq[ActivityData] = Seq()
  ) {
  def toTripDay : TripDay = 
    TripDay(Some(label), activities.map(_.toActivity))
}

object TripDayData {
  def mapping(prefix : String = "") = _mapping(
      "dayNumber" -> of[Int],
      "label" -> of[String],
      "activities" -> seq(ActivityData.mapping())
      )(TripDayData.apply)(TripDayData.unapply).withPrefix(prefix)
  def form(prefix : String = "") = Form(mapping(prefix))
  def fromTripDay(dayNumber : Int, tripDay : TripDay) =
    TripDayData(
        dayNumber,
        tripDay.label.getOrElse(""),
        tripDay.activities.zipWithIndex.map { case (a,idx) => ActivityData.fromActivity(idx,a) }
     )
}  
  
case class TripEditData(
    baseData : TripBaseData,
    tripDays : Seq[TripDayData] = Seq()
    ) {
  def toTrip(tripId : TripId, userId : UserId) =
    Trip(
        ref=Ref(tripId,baseData.name),
        userRef = Ref(userId), 
        regionRef = Ref(baseData.regionId),
        cityRef = Ref(baseData.cityId),
        description = baseData.description,
        days = SortedMap(tripDays.map { td => (td.dayNumber,td.toTripDay) } : _*)
        )
}

object TripEditData {
  def mapping(prefix : String = "") = _mapping(
      "baseData" -> TripBaseData.mapping(),
      "tripDays" -> seq(TripDayData.mapping())
      )(TripEditData.apply)(TripEditData.unapply).withPrefix(prefix)
  def form(prefix : String = "") = Form(mapping(prefix))
  def fromTrip(trip : Trip) : TripEditData = 
    TripEditData(TripBaseData.fromTrip(trip),trip.days.to[Seq].map { case (num,td) => TripDayData.fromTripDay(num,td) })
}    
    

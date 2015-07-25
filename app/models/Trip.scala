package models

import scala.collection.SortedMap
import java.net.URL
import play.api.libs.json._
import play.api.libs.functional.syntax._


case class Ref[T](id : T = java.util.UUID.randomUUID(), name : String = "")

object Ref {
  
  implicit def Format[Id : Format] : Format[Ref[Id]] = (
      (JsPath \ "id").format[Id] and
      (JsPath \ "name").format[String]
    )(Ref.apply, unlift(Ref.unapply))
}

case class Trip(
    ref : Ref[TripId] = Ref(),    
    isPublic : Boolean = false,
    userRef : Ref[UserId],    
    days : SortedMap[Int,TripDay] = SortedMap(),
    description : String = "",
    regionRef : Ref[FeatureId])
    
object JsonTrip {
  
  
  case class RepTrip(
      ref : Ref[TripId],
      isPublic : Boolean,
      userRef : Ref[UserId],
      days : Seq[RepTripDay],
      description : String,
      regionRef : Ref[FeatureId]
      )
  object RepTrip {
    def fromTrip(t : Trip) = 
      RepTrip(
          ref = t.ref,
      isPublic = t.isPublic,
      userRef = t.userRef,
      days = t.days.to[Seq]map({case (num,day) => RepTripDay.fromTripDay(num, day)}),
      description = t.description,
      regionRef = t.regionRef
      )
      implicit def Format : Format[RepTrip] = (
          (JsPath \ "ref").format[Ref[TripId]] and
          (JsPath \ "isPublic").format[Boolean] and
          (JsPath \ "userRef").format[Ref[UserId]] and
          (JsPath \ "days").format[Seq[RepTripDay]] and
          (JsPath \ "description").format[String] and
          (JsPath \ "regionRef").format[Ref[FeatureId]]
      )(RepTrip.apply, unlift(RepTrip.unapply))
  }
  
  case class RepTripDay(
      dayNum : Int,
      description : String,
      activities : Seq[RepActivity]
      )
  
  object RepTripDay {
    implicit def Format : Format[RepTripDay] = (
        (JsPath \ "dayNum").format[Int] and
        (JsPath \ "description").format[String] and
        (JsPath \ "activities").format[Seq[RepActivity]]
    )(RepTripDay.apply,unlift(RepTripDay.unapply))  
  
    def fromTripDay(num : Int,td : TripDay) = {
      RepTripDay(
          dayNum = num,
          description = td.label.getOrElse(""),
          activities = td.activities.zipWithIndex.map({ case (a,i) => RepActivity.fromActivity(i+1,a)})
          )       
    }
  }
  
  case class RepActivity(
      order : Int,
      length : Int,
      description : String,
      actType : String,
      poiRef : Ref[FeatureId],
      fromCityRef : Ref[FeatureId],
      toCityRef : Ref[FeatureId],
      modalityRef : Ref[TransportModalityId]      
      )
  
  object RepActivity {
    implicit def Format : Format[RepActivity] = (
        (JsPath \ "order").format[Int] and
        (JsPath \ "length").format[Int] and
        (JsPath \ "description").format[String] and
        (JsPath \ "actType").format[String] and
        (JsPath \ "poiRef").format[Ref[FeatureId]] and
        (JsPath \ "fromCityRef").format[Ref[FeatureId]] and
        (JsPath \ "toCityRef").format[Ref[FeatureId]] and
        (JsPath \ "modalityRef").format[Ref[TransportModalityId]]        
      )(RepActivity.apply,unlift(RepActivity.unapply))  
  
    def fromActivity(order : Int, a : Activity) : RepActivity =  a match {
      case v : Visit =>
        RepActivity(order,v.lengthHours,v.description,"visit",v.poiRef,Ref(0,""),Ref(0,""),Ref("",""))
      case t : Transport =>
        RepActivity(order,t.lengthHours,t.description,"transport",Ref(0,""),t.fromCity,t.toCity,t.transportModalityRef)
      case u : UndefinedActivity =>
        RepActivity(order,u.lengthHours,u.description,"free",Ref(0,""),Ref(0,""),Ref(0,""),Ref("",""))
    }
  }
    
}    

abstract sealed class FeatureType(val typeSymbol : Char) 

object FeatureType {
  def apply(c : Char) : FeatureType = c match {
    case 'R' => FT_Region
    case 'C' => FT_City
    case 'P' => FT_POI
    case _ => sys.error(s"Invalid FeatureType code: ${c}")
  }
}

case object FT_Region extends FeatureType('R')

case object FT_City extends FeatureType('C')

case object FT_POI extends FeatureType('P')

abstract sealed class URLResource {
  val resource : String
  protected def toUrl(res : String) : String
  lazy val url = new URL(toUrl(resource))
}

final case class DBPediaResource(resource : String) extends URLResource {
  override def toUrl(res : String) = {
    s"""http://dbpedia.org/resource/${res}"""
  }
}
final case class WikipediaResource(resource : String) extends URLResource {
  override def toUrl(res : String) = {
    s"""http://en.wikipedia.org/wiki/${res}"""
  }
}

case class Feature(
    ref : Ref[FeatureId],    
    featureType : FeatureType,
    latitude : Double,
    longitude : Double,
    countryId : Option[Int],
    dbPediaResource : Option[DBPediaResource],
    wikipediaResource : Option[WikipediaResource],
    imageUrl : Option[URL],
    thumbnailUrl : Option[URL],
    description : Option[String],
    superFeatureIds : Set[Ref[FeatureId]],
    subFeatureIds : Set[Ref[FeatureId]]
    )    
        
case class TripDay (       
    label : Option[String] = None,
    activities : Seq[Activity] = Seq()
    )

sealed trait Activity {
  val lengthHours : Int
  val description : String
}

case class UndefinedActivity(lengthHours : Int = 1) extends Activity {
  val description = ""
}

case class Visit(    
    poiRef : Ref[FeatureId],
    description : String = "",
    lengthHours : Int = 1
    ) extends Activity

case class Transport(    
    fromCity : Ref[FeatureId],
    toCity : Ref[FeatureId],
    transportModalityRef : Ref[TransportModalityId],
    description : String = "",
    lengthHours : Int = 1
    ) extends Activity    
    
case class TransportModality(
    ref : Ref[TransportModalityId]
    )
package models

import scala.collection.SortedMap

import java.net.URL
import play.api.libs.json._
import play.api.libs.json.util._
import play.api.libs.json.Reads._
import play.api.libs.json.Writes._
import play.api.libs.json.Format._
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
    regionRef : Ref[FeatureId],
    cityRef: Ref[FeatureId]) {
  def endsIn : Option[Ref[FeatureId]] = {
    days.values.foldLeft[Option[Ref[FeatureId]]](Some(cityRef)) {
      case (cr,day) =>
        day.endsIn.orElse(cr)
    }
  }
}
    
object JsonTrip {
  
  
  case class RepTrip(
      ref : Ref[TripId],
      isPublic : Boolean,
      userRef : Ref[UserId],
      days : Seq[RepTripDay],
      description : String,
      regionRef : Ref[FeatureId],
      cityRef : Ref[FeatureId]
      ) {
    def toTrip = {
      Trip(
          ref = ref,
          isPublic = isPublic,
          userRef = userRef,
          description = description,
          regionRef = regionRef,
          cityRef = cityRef,
          days = SortedMap(days.map(d => (d.num,d.toTripDay)):_*)
      )
    }
  }
  object RepTrip {
    def fromTrip(t : Trip) = 
      RepTrip(
          ref = t.ref,
      isPublic = t.isPublic,
      userRef = t.userRef,
      days = t.days.to[Seq]map({case (num,day) => RepTripDay.fromTripDay(num, day)}),
      description = t.description,
      regionRef = t.regionRef,
      cityRef = t.cityRef
      )
    
    implicit def Format : Format[RepTrip] = (
          (JsPath \ "ref").format[Ref[TripId]] and
          (JsPath \ "isPublic").format[Boolean] and
          (JsPath \ "userRef").format[Ref[UserId]] and
          (JsPath \ "days").format[Seq[RepTripDay]] and
          (JsPath \ "description").format[String] and
          (JsPath \ "regionRef").format[Ref[FeatureId]] and
          (JsPath \ "cityRef").format[Ref[FeatureId]]
      )(RepTrip.apply, unlift(RepTrip.unapply))
  }
  
  case class RepTripDay(
      num : Int,
      description : String,
      activities : Seq[RepActivity]
      ) {
    def toTripDay = {
      val desc = description.trim 
      TripDay(          
          label = if(desc.isEmpty()) { None } else { Some(desc) },
          activities = activities.sortBy(_.order).map(_.toActivity)
          )
    }
  }
  
  object RepTripDay {
    implicit def Format : Format[RepTripDay] = (
        (JsPath \ "num").format[Int] and
        (JsPath \ "description").format[String] and
        (JsPath \ "activities").format[Seq[RepActivity]]
    )(RepTripDay.apply,unlift(RepTripDay.unapply))  
  
    def fromTripDay(num : Int,td : TripDay) = {
      RepTripDay(
          num = num,
          description = td.label.getOrElse(""),
          activities = td.activities.zipWithIndex.map({ case (a,i) => RepActivity.fromActivity(i+1,a)})
          )       
    }
  }
  
  case class RepActivity(
      order : Int,
      length : Int,
      description : Option[String] = None,
      actType : String,
      poiRef : Option[Ref[FeatureId]] = None,      
      toCityRef : Option[Ref[FeatureId]] = None,
      modalityRef : Option[Ref[TransportModalityId]] = None      
      ) {
    def toActivity : Activity = actType match {
      case "visit" => Visit(poiRef.get,description.getOrElse(""),length)
      case "transport" => Transport(toCityRef.get,modalityRef.get,description.getOrElse(""),length)
      case "free" => UndefinedActivity(length,description.getOrElse(""))
    }
  }
  
  object RepActivity {
    implicit def Format : Format[RepActivity] = (
        (JsPath \ "order").format[Int] and
        (JsPath \ "length").format[Int] and
        (JsPath \ "description").formatNullable[String] and
        (JsPath \ "type").format[String] and
        (JsPath \ "poiRef").formatNullable[Ref[FeatureId]] and        
        (JsPath \ "toCityRef").formatNullable[Ref[FeatureId]] and
        (JsPath \ "modalityRef").formatNullable[Ref[TransportModalityId]]        
      )(RepActivity.apply,unlift(RepActivity.unapply))  
  
    def fromActivity(order : Int, a : Activity) : RepActivity =  a match {
      case v : Visit =>
        RepActivity(order,v.lengthHours,Some(v.description),"visit",Some(v.poiRef),None,None)
      case t : Transport =>
        RepActivity(order,t.lengthHours,Some(t.description),"transport",None,Some(t.toCity),Some(t.transportModalityRef))
      case u : UndefinedActivity =>
        RepActivity(order,u.lengthHours,Some(u.description),"free",None,None,None)
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
    ) {
  def endsIn : Option[Ref[FeatureId]] = {
    activities.foldLeft[Option[Ref[FeatureId]]](None) {
      case (cr,act) =>
        act.endsIn.orElse(cr)
    }
  }
}

sealed trait Activity {
  val lengthHours : Int
  val description : String
  def endsIn : Option[Ref[FeatureId]] = None
}

case class UndefinedActivity(lengthHours : Int = 1, description : String = "") extends Activity {
  
}

case class Visit(    
    poiRef : Ref[FeatureId],
    description : String = "",
    lengthHours : Int = 1
    ) extends Activity

case class Transport(        
    toCity : Ref[FeatureId],
    transportModalityRef : Ref[TransportModalityId],
    description : String = "",
    lengthHours : Int = 1
    ) extends Activity    {
  override def endsIn : Option[Ref[FeatureId]] = Some(toCity)
}
    
case class TransportModality(
    ref : Ref[TransportModalityId]
    )
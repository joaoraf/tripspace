 package models.daos.slickdaos.dbobjs

case class DBTrip(
    tripId : String,        
    tripName : String,
    tripDescription : String,
    tripIsPublic : Boolean,
    userId : String,
    regionId : Int
    )
                 
                
case class DBTripDay (        
    tripId : String,                
    dayNumber : Int,
    label : Option[String] = None    
    )
           
 
abstract sealed class DBBaseActivity {
  val tripId : String
  val dayNumber : Int
  val order : Int
}

case class DBActivity(
    tripId : String,
    dayNumber : Int,
    order : Int,
    lengthHours : Int) extends DBBaseActivity
    
case class DBVisitActivity(
    tripId : String,
    dayNumber : Int,
    order : Int,
    visitCityId : Int,
    visitDescription : String
    )  extends DBBaseActivity

case class DBTransportActivity(
    tripId : String,
    dayNumber : Int,
    order : Int,
    fromCityId : Int,
    toCityId : Int,
    transportModalityId : String,
    transportDescription : String
    )  extends DBBaseActivity  
    
case class DBTransportModality(
    transportModalityId : String,
    transportModalityName : String
    )

case class DBFeature(
    id : Int,
    name : String,
    latitude : Double,
    longitude : Double,
    countryId : Option[Int] = None,
    dbpediaResource : Option[String] = None,
    wikipediaResource : Option[String] = None,
    imageUrl : Option[String] = None,
    thumbnailUrl : Option[String] = None,
    description : Option[String] = None,
    featureType : Char
)
    
case class DBFeatureHierarchy(
    botId : Int,
    topId : Int)     

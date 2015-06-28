package models.daos.slickdaos.dbobjs

case class DBTrip(
    tripId : String,        
    tripName : String,
    tripIsPublic : Boolean,
    userId : String,
    regionId : String
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
    visitCityId : String,
    visitDescription : String
    )  extends DBBaseActivity

case class DBTransportActivity(
    tripId : String,
    dayNumber : Int,
    order : Int,
    fromCityId : String,
    toCityId : String,
    transportModalityId : String,
    transportDescription : String
    )  extends DBBaseActivity  
    
case class DBTransportModality(
    transportModalityId : String,
    transportModalityName : String
    )

case class DBCity(
    cityId : String,
    cityName : String,
    cityDescription : String,
    regionId : String
    )

case class DBPOI(
    poiId : String,
    poiName : String,
    poiDescription : String,
    cityId : String)    
    
case class DBRegion(
    regionId : String,
    regionName : String,
    regionDescription : String,
    regionThumbnail : Option[String],
    superRegionId : Option[String]
    )
    

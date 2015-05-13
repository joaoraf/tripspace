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
    visitPlaceId : String,
    visitDescription : String
    )  extends DBBaseActivity

case class DBTransportActivity(
    tripId : String,
    dayNumber : Int,
    order : Int,
    fromPlaceId : String,
    toPlaceId : String,
    transportModalityId : String,
    transportDescription : String
    )  extends DBBaseActivity  
    
case class DBTransportModality(
    transportModalityId : String,
    transportModalityName : String
    )

case class DBPlace(
    placeId : String,
    placeName : String,
    placeDescription : String
    )

case class DBPlaceRegion(
    placeId : String,
    regionId : String
    )

case class DBRegion(
    regionId : String,
    regionName : String,
    regionDescription : String,
    regionThumbnail : Option[String]
    )
    
case class DBRegionSubRegion(
    superRegionId : String,
    subRegionId : String
        )

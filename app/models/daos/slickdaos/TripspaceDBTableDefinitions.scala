package models.daos.slickdaos

import slick.driver.PostgresDriver.api._
import scala.language.postfixOps


import ForeignKeyAction._

trait TripspaceDBTableDefinitions {    
    import models.daos.slickdaos.dbobjs._  
      
            
    class Trips(tag : Tag) extends Table[DBTrip](tag,"trip") {
      def tripId = column[String]("trip_id", O.PrimaryKey)
      def tripName = column[String]("trip_name", O.Default(""))
      def tripIsPublic = column[Boolean]("trip_is_public", O.Default(false))      
      def userId = column[String]("user_id")
      def regionId = column[String]("region_id")
      
      foreignKey("fk_trip_user", userId, slickUsers)(_.id, Cascade,Restrict)
      foreignKey("fk_trip_region", tripId, slickRegions)(_.regionId, Cascade,Restrict)
      
      def * = (tripId,tripName,tripIsPublic,userId,regionId) <> (DBTrip.tupled,DBTrip.unapply)
    }
    
    val slickTrips = TableQuery[Trips]
        
    class Regions(tag : Tag) extends Table[DBRegion](tag, "region") {
      def regionId = column[String]("region_id", O.PrimaryKey)
      def regionName = column[String]("region_name", O.Default(""))
      def regionDescription = column[String]("region_description", O.Default(""))
      def regionThumbnail = column[Option[String]]("region_thumbnail")
      def superRegionId = column[Option[String]]("super_region_id", O.Default(None))
      
      foreignKey("fk_region_super_region", superRegionId,slickRegions)(_.regionId.?,Cascade,Cascade)
      
      def * = (regionId, regionName, regionDescription, regionThumbnail, superRegionId) <> (DBRegion.tupled, DBRegion.unapply)
    }    
    
    val slickRegions = TableQuery[Regions]
     
    class TripDays(tag : Tag) extends Table[DBTripDay](tag, "trip_day") {      
      def tripId = column[String]("trip_id")
      def dayNumber = column[Int]("day_number")
      def label = column[Option[String]]("day_label")
      
      primaryKey("fk_day", (tripId, dayNumber))
      foreignKey("fk_trip_days_trip", tripId, slickTrips)(_.tripId, Cascade,Cascade)
      index("fk_trip_days_unique_day_number", (tripId,dayNumber), true)
      
      def * = (tripId, dayNumber, label) <> (DBTripDay.tupled, DBTripDay.unapply)
    }
    
    val slickTripDays = TableQuery[TripDays]
    
    abstract sealed class BaseActivityTable[T <: DBBaseActivity](tag : Tag, name : String) extends Table[T](tag,name) {
      def tripId : Rep[String]
      def dayNumber : Rep[Int]
      def order : Rep[Int]
    }
    
    type BaseActivityTableQuery[T <: BaseActivityTable[_ <: DBBaseActivity]] = TableQuery[T]
    
    class Activities(tag : Tag) extends BaseActivityTable[DBActivity](tag, "activity") {
      def tripId = column[String]("trip_id")
      def dayNumber = column[Int]("day_number")
      def order = column[Int]("activity_order")
      def lengthHours = column[Int]("length_hours")
      
      foreignKey("fk_activity_trip_day", (tripId,dayNumber), slickTripDays)(x => (x.tripId, x.dayNumber), Cascade,Cascade)
      primaryKey("pk_activity", (tripId,dayNumber,order))
      
      def * = (tripId,dayNumber,order,lengthHours) <> (DBActivity.tupled, DBActivity.unapply)
    }
        
    val slickActivities : BaseActivityTableQuery[Activities] = TableQuery[Activities] 
    
    class VisitActivity(tag : Tag) extends BaseActivityTable[DBVisitActivity](tag, "visit_activity") {
      def tripId = column[String]("trip_id")
      def dayNumber = column[Int]("day_number")
      def order = column[Int]("activity_order")
      def visitPOIId = column[String]("visitPOIId")
      def visitDescription = column[String]("visit_description", O.Default(""))      
      
      foreignKey("fk_visit_activity_day", (tripId,dayNumber,order), slickActivities)(x => (x.tripId, x.dayNumber, x.order), Cascade,Cascade)
      foreignKey("fk_visit_activity_city", visitPOIId, slickPointsOfInterest)(_.poiId,Cascade,Cascade)
      
      primaryKey("pk_visit_activity", (tripId,dayNumber,order))                  
      
      def * = (tripId,dayNumber,order,visitPOIId,visitDescription) <> (DBVisitActivity.tupled, DBVisitActivity.unapply)
    }
    
    val slickVisitActivities : BaseActivityTableQuery[VisitActivity] = TableQuery[VisitActivity]
    
    class TransportActivity(tag : Tag) extends BaseActivityTable[DBTransportActivity](tag, "transport_activity") {
      def tripId = column[String]("trip_id")
      def dayNumber = column[Int]("day_number")
      def order = column[Int]("activity_order")
      
      def fromCityId = column[String]("fromCityId")
      def toCityId = column[String]("toCityId")
      def transportModalityId = column[String]("transport_modality_id")
      def transportDescription = column[String]("transport_description", O.Default(""))
      foreignKey("fk_transport_activity_from_city", fromCityId, slickCities)(_.cityId,Cascade,Cascade)
      foreignKey("fk_transport_activity_to_city", toCityId, slickCities)(_.cityId,Cascade,Cascade)
      foreignKey("fk_transport_activity_modality", transportModalityId, transportModalities)(_.transportModalityId,Cascade,Cascade)
            
      foreignKey("fk_transport_activity_day", (tripId,dayNumber,order), slickActivities)(x => (x.tripId, x.dayNumber, x.order), Cascade,Cascade)      
      
      primaryKey("pk_transport_activity", (tripId,dayNumber,order))          
      
      def * = (tripId,dayNumber,order,fromCityId,toCityId,transportModalityId,transportDescription) <> (DBTransportActivity.tupled, DBTransportActivity.unapply)
    }
    
    val slickTransportActivities : BaseActivityTableQuery[TransportActivity] = TableQuery[TransportActivity]            
    
    class Cities(tag : Tag) extends Table[DBCity](tag, "city") {
      def cityId = column[String]("cityId", O.PrimaryKey)
      def cityName = column[String]("cityName")
      def cityDescription = column[String]("cityDescription", O.Default(""))
      def regionId = column[String]("region_id")
      
      foreignKey("fk_city_region", regionId, slickRegions)(_.regionId,Cascade,Cascade)
      
      def * = (cityId,cityName,cityDescription,regionId) <> (DBCity.tupled,DBCity.unapply)
    }
    
    val slickCities = TableQuery[Cities]
    
    class PointsOfInterest(tag : Tag) extends Table[DBPOI](tag,"poi") {
      def poiId = column[String]("poiId", O.PrimaryKey)
      def poiName = column[String]("poiName")
      def poiDescription = column[String]("poiDescription", O.Default(""))
      def cityId = column[String]("cityId")
      
      foreignKey("fk_poi_city", cityId, slickCities)(_.cityId,Cascade,Cascade)
      
      def * = (poiId, poiName, poiDescription, cityId) <> (DBPOI.tupled,DBPOI.unapply)
    }
    
    val slickPointsOfInterest = TableQuery[PointsOfInterest]
    
    class TransportModalities(tag :Tag) extends Table[DBTransportModality](tag, "transport_modality") {
      def transportModalityId = column[String]("transport_modality_id",O.PrimaryKey)
      def transportModalityName = column[String]("transport_modality_name")
      
      def * = (transportModalityId, transportModalityName) <> (DBTransportModality.tupled, DBTransportModality.unapply)
    }
    
    val transportModalities = TableQuery[TransportModalities]
    
    
    
    /*
     * Silhouette tables
     */
        
    case class DBUser (
      userID: String,
      firstName: Option[String],
      lastName: Option[String],
      fullName: Option[String],
      email: Option[String],
      avatarURL: Option[String]
    )
  
    class Users(tag: Tag) extends Table[DBUser](tag, "user") {
      def id = column[String]("userID", O.PrimaryKey)
      def firstName = column[Option[String]]("firstName")
      def lastName = column[Option[String]]("lastName")
      def fullName = column[Option[String]]("fullName")
      def email = column[Option[String]]("email")
      def avatarURL = column[Option[String]]("avatarURL")
      def * = (id, firstName, lastName, fullName, email, avatarURL) <> (DBUser.tupled, DBUser.unapply)
    }
  
    case class DBLoginInfo (
      id: Option[Long],
      providerID: String,
      providerKey: String
    )
  
    class LoginInfos(tag: Tag) extends Table[DBLoginInfo](tag, "logininfo") {
      def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
      def providerID = column[String]("providerID")
      def providerKey = column[String]("providerKey")
      def * = (id.?, providerID, providerKey) <> (DBLoginInfo.tupled, DBLoginInfo.unapply)
    }
  
    case class DBUserLoginInfo (
      userID: String,
      loginInfoId: Long
    )
  
    class UserLoginInfos(tag: Tag) extends Table[DBUserLoginInfo](tag, "userlogininfo") {
      def userID = column[String]("userID")
      def loginInfoId = column[Long]("loginInfoId")
      def * = (userID, loginInfoId) <> (DBUserLoginInfo.tupled, DBUserLoginInfo.unapply)
    }
  
    case class DBPasswordInfo (
      hasher: String,
      password: String,
      salt: Option[String],
      loginInfoId: Long
    )
  
    class PasswordInfos(tag: Tag) extends Table[DBPasswordInfo](tag, "passwordinfo") {
      def hasher = column[String]("hasher")
      def password = column[String]("password")
      def salt = column[Option[String]]("salt")
      def loginInfoId = column[Long]("loginInfoId")
      def * = (hasher, password, salt, loginInfoId) <> (DBPasswordInfo.tupled, DBPasswordInfo.unapply)
    }
  
    case class DBOAuth1Info (
      id: Option[Long],
      token: String,
      secret: String,
      loginInfoId: Long
    )
  
    class OAuth1Infos(tag: Tag) extends Table[DBOAuth1Info](tag, "oauth1info") {
      def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
      def token = column[String]("token")
      def secret = column[String]("secret")
      def loginInfoId = column[Long]("loginInfoId")
      def * = (id.?, token, secret, loginInfoId) <> (DBOAuth1Info.tupled, DBOAuth1Info.unapply)
    }
  
    case class DBOAuth2Info (
      id: Option[Long],
      accessToken: String,
      tokenType: Option[String],
      expiresIn: Option[Int],
      refreshToken: Option[String],
      loginInfoId: Long
    )
  
    class OAuth2Infos(tag: Tag) extends Table[DBOAuth2Info](tag, "oauth2info") {
      def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
      def accessToken = column[String]("accesstoken")
      def tokenType = column[Option[String]]("tokentype")
      def expiresIn = column[Option[Int]]("expiresin")
      def refreshToken = column[Option[String]]("refreshtoken")
      def loginInfoId = column[Long]("logininfoid")
      def * = (id.?, accessToken, tokenType, expiresIn, refreshToken, loginInfoId) <> (DBOAuth2Info.tupled, DBOAuth2Info.unapply)
    }
  
    val slickUsers = TableQuery[Users]
    val slickLoginInfos = TableQuery[LoginInfos]
    val slickUserLoginInfos = TableQuery[UserLoginInfos]
    val slickPasswordInfos = TableQuery[PasswordInfos]
    val slickOAuth1Infos = TableQuery[OAuth1Infos]
    val slickOAuth2Infos = TableQuery[OAuth2Infos]
  
    
    val allTables = Seq(
        slickUsers,slickLoginInfos,slickUserLoginInfos,slickPasswordInfos,
        slickOAuth1Infos, slickOAuth2Infos, transportModalities, slickRegions, slickCities, 
        slickTrips, slickTripDays, slickActivities, slickVisitActivities, 
        slickTransportActivities, slickPointsOfInterest)
}
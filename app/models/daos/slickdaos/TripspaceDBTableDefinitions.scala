package models.daos.slickdaos

import slick.driver.PostgresDriver.api._
import scala.language.postfixOps


import ForeignKeyAction._

trait TripspaceDBTableDefinitions {    
    import models.daos.slickdaos.dbobjs._  
      
            
    class Trips(tag : Tag) extends Table[DBTrip](tag,"trip") {
      def tripId = column[String]("trip_id", O.PrimaryKey)
      def tripName = column[String]("trip_name", O.Default(""))
      def tripDescription = column[String]("trip_description", O.Default(""))
      def tripIsPublic = column[Boolean]("trip_is_public", O.Default(false))      
      def userId = column[String]("user_id")
      def regionId = column[Int]("region_id")
      
      lazy val fk_user = foreignKey("fk_trip_user", userId, slickUsers)(_.id, Cascade,Restrict)
      lazy val fk_region = foreignKey("fk_trip_region", regionId, features)(_.id, Cascade,Restrict)
      
      def * = (tripId,tripName,tripDescription,tripIsPublic,userId,regionId) <> (DBTrip.tupled,DBTrip.unapply)
    }
    
    lazy val slickTrips = TableQuery[Trips]
        
   
     
    class TripDays(tag : Tag) extends Table[DBTripDay](tag, "trip_day") {      
      def tripId = column[String]("trip_id")
      def dayNumber = column[Int]("day_number")
      def label = column[Option[String]]("day_label")
      
      lazy val pk = primaryKey("fk_day", (tripId, dayNumber))
      lazy val fk_trip = foreignKey("fk_trip_days_trip", tripId, slickTrips)(_.tripId, Cascade,Cascade)
      
      def * = (tripId, dayNumber, label) <> (DBTripDay.tupled, DBTripDay.unapply)
    }
    
    lazy val slickTripDays = TableQuery[TripDays]
    
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
      
      val fk_trip_day = foreignKey("fk_activity_trip_day", (tripId,dayNumber), slickTripDays)(x => (x.tripId, x.dayNumber), Cascade,Cascade)
      val pk = primaryKey("pk_activity", (tripId,dayNumber,order))
      
      def * = (tripId,dayNumber,order,lengthHours) <> (DBActivity.tupled, DBActivity.unapply)
    }
        
    lazy val slickActivities : BaseActivityTableQuery[Activities] = TableQuery[Activities] 
    
    class VisitActivity(tag : Tag) extends BaseActivityTable[DBVisitActivity](tag, "visit_activity") {
      def tripId = column[String]("trip_id")
      def dayNumber = column[Int]("day_number")
      def order = column[Int]("activity_order")
      def visitPOIId = column[Int]("visit_poi_id")
      def visitDescription = column[String]("visit_description", O.Default(""))      
      
      lazy val fk_activity = foreignKey("fk_visit_activity_day", (tripId,dayNumber,order), slickActivities)(x => (x.tripId, x.dayNumber, x.order), Cascade,Cascade)
      lazy val fk_poi = foreignKey("fk_visit_activity_poi", visitPOIId, features)(_.id,Cascade,Cascade)
      
      val pk = primaryKey("pk_visit_activity", (tripId,dayNumber,order))                  
      
      def * = (tripId,dayNumber,order,visitPOIId,visitDescription) <> (DBVisitActivity.tupled, DBVisitActivity.unapply)
    }
    
    lazy val slickVisitActivities : BaseActivityTableQuery[VisitActivity] = TableQuery[VisitActivity]
    
    class TransportActivity(tag : Tag) extends BaseActivityTable[DBTransportActivity](tag, "transport_activity") {
      def tripId = column[String]("trip_id")
      def dayNumber = column[Int]("day_number")
      def order = column[Int]("activity_order")
      
      def fromCityId = column[Int]("from_city_id")
      def toCityId = column[Int]("to_city_id")
      def transportModalityId = column[String]("transport_modality_id")
      def transportDescription = column[String]("transport_description", O.Default(""))
      lazy val fk_from_city = foreignKey("fk_transport_activity_from_city", fromCityId, features)(_.id,Cascade,Cascade)
      lazy val fk_to_city = foreignKey("fk_transport_activity_to_city", toCityId, features)(_.id,Cascade,Cascade)
      lazy val fk_modality = foreignKey("fk_transport_activity_modality", transportModalityId, transportModalities)(_.transportModalityId,Cascade,Cascade)
            
      lazy val fk_activity = foreignKey("fk_transport_activity_day", (tripId,dayNumber,order), slickActivities)(x => (x.tripId, x.dayNumber, x.order), Cascade,Cascade)      
      
      lazy val pk = primaryKey("pk_transport_activity", (tripId,dayNumber,order))          
      
      def * = (tripId,dayNumber,order,fromCityId,toCityId,transportModalityId,transportDescription) <> (DBTransportActivity.tupled, DBTransportActivity.unapply)
    }
    
    lazy val slickTransportActivities : BaseActivityTableQuery[TransportActivity] = TableQuery[TransportActivity]            
        
    
    class TransportModalities(tag :Tag) extends Table[DBTransportModality](tag, "transport_modality") {
      def transportModalityId = column[String]("transport_modality_id",O.PrimaryKey)
      def transportModalityName = column[String]("transport_modality_name")
      
      def * = (transportModalityId, transportModalityName) <> (DBTransportModality.tupled, DBTransportModality.unapply)
    }
    
    lazy val transportModalities = TableQuery[TransportModalities]
    
    class Features(tag : Tag) extends Table[DBFeature](tag, "feature") {
      def id = column[Int]("id")
      def name = column[String]("name")
      def latitude = column[Double]("latitude")
      def longitude = column[Double]("longitude")
      def countryId = column[Option[Int]]("country_id")
      def dbpediaResource = column[Option[String]]("dbpedia_resource")
      def wikipediaResource = column[Option[String]]("wikipedia_resource")
      def imageUrl = column[Option[String]]("image_url")
      def thumbnailUrl = column[Option[String]]("thumbnail_url")
      def description = column[Option[String]]("description")
      def featureType = column[Char]("feature_type")
      
      def * = (id,name,latitude,longitude,countryId,dbpediaResource,wikipediaResource,imageUrl,thumbnailUrl,description,featureType) <>
              (DBFeature.tupled,DBFeature.unapply)
    }
    
    lazy val features = TableQuery[Features]
    
    class FeatureHierarchies(tag : Tag) extends Table[DBFeatureHierarchy](tag, "feature_hierarchy") {
      def botId = column[Int]("bot_id")
      def topId = column[Int]("top_id")
      
      def * = (botId,topId) <> (DBFeatureHierarchy.tupled, DBFeatureHierarchy.unapply)
    }
    
    lazy val featureHierarchies = TableQuery[FeatureHierarchies]
    
    
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
  
    lazy val slickUsers = TableQuery[Users]
    lazy val slickLoginInfos = TableQuery[LoginInfos]
    lazy val slickUserLoginInfos = TableQuery[UserLoginInfos]
    lazy val slickPasswordInfos = TableQuery[PasswordInfos]
    lazy val slickOAuth1Infos = TableQuery[OAuth1Infos]
    lazy val slickOAuth2Infos = TableQuery[OAuth2Infos]
  
    
    lazy val allTables = Seq(
        slickUsers,slickLoginInfos,slickUserLoginInfos,slickPasswordInfos,
        slickOAuth1Infos, slickOAuth2Infos, transportModalities, features, featureHierarchies,
        slickTrips, slickTripDays, slickActivities, slickVisitActivities, 
        slickTransportActivities)
    
    lazy val createStatements = allTables.flatMap(_.schema.createStatements).to[IndexedSeq]
}
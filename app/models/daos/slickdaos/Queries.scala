package models.daos.slickdaos

import java.util.UUID

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.language.implicitConversions
import scala.language.existentials
import scala.language.higherKinds
import scala.language.postfixOps
import DBTableDefinitions._
import javax.inject.Inject
import models._
import models.daos._
import play.api.db.slick._
import slick.backend.DatabaseConfig
import slick.driver.PostgresDriver.api._
import slick.lifted.CanBeQueryCondition
import slick.profile.RelationalProfile
import play.api.db.DB
import scalaz._
import std.option._
import slick.driver.PostgresDriver
import scala.collection.generic.CanBuildFrom
import slick.ast.BaseTypedType
import scala.collection.SortedMap
import _root_.utils.collections._
import java.lang.Class.Atomic
import akka.agent.Agent
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.Logger
import slick.lifted.AbstractTable


 
class SlickQueries @Inject() (dbConfigProvider : DatabaseConfigProvider)  {
  import dbobjs._
  object Implicits {
   /* implicit def dbRegionToRegion(dbRegion : DBRegion) : Region = 
      Region(dbRegion.regionId, dbRegion.regionName,
           dbRegion.regionDescription, dbRegion.regionThumbnail) */
           
    implicit def regionToDbRegion(region : Region) : DBRegion = 
      DBRegion(region.ref.id, region.ref.name,
             region.description, region.thumbnail, region.optSuperRegionRef.map(_.id))
             
    implicit def cityToDBCity(city : City) : DBCity = 
      DBCity(city.ref.id,city.ref.name,city.description,city.regionRef.id)
      
    implicit def dbTranspModToDomain(t : DBTransportModality) =
        TransportModality(Ref(t.transportModalityId,t.transportModalityName))
        
    implicit def domainTranspModToDb(t : TransportModality) =
        DBTransportModality(t.ref.id.toString,t.ref.name)
  }
  
  import Implicits._

  lazy val dbConfig = dbConfigProvider.get[RelationalProfile]
  
  import dbConfig.driver.api._  
  implicit val db = dbConfig.db       
      
  def exists_[T <: AbstractTable[_], R <: Rep[_]](table : TableQuery[T])(f: T => R)(implicit wt: CanBeQueryCondition[R]) = 
      table.filter(f).exists.result
    
  object region {          
    def makeRef(dbRegion : DBRegion) : Ref[RegionId] = Ref(dbRegion.regionId, dbRegion.regionName) 
                
    def toRegion(dbRegion : DBRegion, optSuperRegionRef : Option[Ref[RegionId]] = None,
        setSubRegionRefs : Set[Ref[RegionId]] = Set(),
        setCityRefs : Set[Ref[CityId]] = Set()) : Region = 
      Region(makeRef(dbRegion), dbRegion.regionDescription, 
             dbRegion.regionThumbnail, optSuperRegionRef, setSubRegionRefs,
             setCityRefs)
    
    def fill(ids : Set[String])(implicit ec : ExecutionContext) : DBIO[Map[String,Region]] = for {
       dbRegions <- (for {
           (dbRegion,optDbSuperRegion) <- 
             slickRegions joinLeft slickRegions on {
               case (r,sr) => r.superRegionId === sr.regionId
             }
           if dbRegion.regionId inSet ids 
         } yield (dbRegion.regionId,(dbRegion,optDbSuperRegion))).result
       dbRegionMap = dbRegions.toMap.mapValues {
         case (r,dbsr) => toRegion(r,dbsr.map(r => Ref(r.regionId,r.regionName)))
       }
       subRegionSeq <- (for {
         region <- slickRegions
         if region.superRegionId inSet ids
       } yield (
           (region.superRegionId.get,(region.regionId,region.regionName))
           )).result
       subRegionMap = 
         subRegionSeq
             .groupBy({case (x,_) => x})
             .mapValues(_.map { case (_,(id,n)) => Ref(id,n) }.to[Set])
             .withDefault(_ => Set[Ref[RegionId]]())
             
       cityMap <- for {
         cities <- 
           slickCities
             .filter(_.regionId inSet ids)
             .map(c => (c.regionId,(c.cityId,c.cityName)))            
             .result
       } yield (
           cities.groupBy(_._1)
                 .mapValues(
                     _.map(p => Ref(p._2._1,p._2._2))
                      .to[Set])
                 .withDefault(_ => Set[Ref[CityId]]()))
       
       res = mergeMaps3_F(dbRegionMap, subRegionMap, cityMap) { 
          case (_,region,subRegions,cities) =>
            region copy (
                setSubRegionRefs =subRegions,
                setCityRefs = cities) 
       }
    } yield (res)
         
    def find(regionId : RegionId)(implicit ec : ExecutionContext) : DBIO[Option[Region]] = 
      fill(Set(regionId)).map(_.values.headOption)
                        
    def exists(regionId : RegionId)(implicit ec : ExecutionContext) : DBIO[Boolean] =
      slickRegions.filter(_.regionId === regionId).exists.result
         
    def save(region : Region)(implicit ec : ExecutionContext) = for {
        regionExists <- exists(region.ref.id)
        _ <- if (regionExists) { for {
            _ <- slickRegions.filter(_.regionId === region.ref.id).update(region)            
            _ <- slickRegions
                    .filter(r => r.superRegionId.get === region.ref.id &&
                            ! (r.regionId inSet region.setCityRefs.map(_.id))).delete                    
          } yield ()
        } else {
          slickRegions += region
        }       
    } yield (region)
  }
  
  
  object city {       
    
    def makeRef(dbCity : DBCity) : Ref[CityId] = Ref(dbCity.cityId,dbCity.cityName)
    
    def toCity(dbCity : DBCity, regionRef : Ref[RegionId], poiRefs : Set[Ref[POIId]] = Set()) =
      City(makeRef(dbCity), dbCity.cityDescription, regionRef, poiRefs)
    
    def fill(cityIds : Set[String], deep : Boolean = true)(implicit ec : ExecutionContext) : DBIO[Map[String,City]] = for {
      dbCities <- (for {
        city <- slickCities        
        if city.cityId inSet cityIds
        region <- slickRegions
        if region.regionId === city.regionId
      } yield (city.cityId, (city,(region.regionId,region.regionName)))).result
      preCityMap = dbCities.toMap.mapValues { case (c,(rid,rn)) => toCity(c,Ref(rid,rn))}
   
      poiSeq <- (for {
          dbPoi <- slickPointsOfInterest
          if (dbPoi.cityId inSet cityIds)
        } yield (dbPoi.cityId, (dbPoi.poiId, dbPoi.poiName))).result
      poiMap = poiSeq.groupBy(_._1).mapValues(
          _.map { case (_,(id,n)) => Ref(id,n) }.to[Set])                 
      cityMap = mergeMaps2_F(preCityMap, poiMap) {
        case (_,city,ps) => city copy (poiRefs = ps)
      }
    } yield (cityMap)
    
    def find(cityId : CityId)(implicit ec : ExecutionContext) : DBIO[Option[City]]= 
      fill(Set(cityId)).map(_.values.headOption)
    
    def exists(cityId : CityId)(implicit ec : ExecutionContext) : DBIO[Boolean] = 
      slickCities.filter(_.cityId === cityId).exists.result
              
    def save(city : City)(implicit ec : ExecutionContext) : DBIO[City] = for {
      cityExists <- exists(city.ref.id)
      _ <- if (cityExists) { for {
             _ <- slickCities.filter(_.cityId === city.ref.id).update(city)
             _ <- slickPointsOfInterest.filter( 
                 p => p.cityId === city.ref.id &&
                      !(p.poiId inSet city.poiRefs.map(_.id).toSet)).delete
             } yield (())               
           } else {
             slickCities += city
           }        
    } yield (city)          
  }
  
  object poi {
    def toPOI(dbPOI : DBPOI, city : City) : POI = ???
    def makeRef(dbPOI : DBPOI) : Ref[POIId] = Ref(dbPOI.poiId,dbPOI.poiName)
  }
   
  object transportModality {
    def makeRef(dbTM : DBTransportModality) : Ref[TransportModalityId] = ???
    
    def allModalities()(implicit ec : ExecutionContext) : DBIO[Map[String,TransportModality]] = 
      transportModalities.result.map(_.map(t => (t.transportModalityId,t : TransportModality)).toMap)
    
    def find(transportModalityId : TransportModalityId)(implicit ec : ExecutionContext) : DBIO[Option[TransportModality]]=  
      transportModalities.filter(_.transportModalityId === transportModalityId)
                         .result.headOption.map(_.map(x => x))
          
    def exists(transportModalityId : TransportModalityId)(implicit ec : ExecutionContext) : DBIO[Boolean] = 
      transportModalities.filter(_.transportModalityId === transportModalityId).exists.result
                  
    def save(transportModality : TransportModality)(implicit ec : ExecutionContext) : DBIO[TransportModality] = for {
      tmExists <- exists(transportModality.ref.id)
      _ <- if(tmExists) { 
             transportModalities
              .filter(_.transportModalityId === transportModality.ref.id)
              .update(transportModality)
           } else { 
             transportModalities += transportModality
           }         
    } yield (transportModality)
    
  }
  
  object trip {                             
    def makeRef(dbTrip : DBTrip) : Ref[TripId] = Ref(UUID.fromString(dbTrip.tripId),dbTrip.tripName) 
    
    def tripsToRegions(tripIds : Set[String])(implicit ec : ExecutionContext) : DBIO[Map[String,Region]] = for {
      tripRegionPairs <- (for {
        tr <- slickTrips
        if tr.tripId inSet tripIds
        r = (tr.tripId, tr.regionId)
      } yield (r)).result
                     
      regionIds = tripRegionPairs.map(_._2).toSet
      regions <- region.fill(regionIds)
      tripRegions = tripRegionPairs map { case(tripId,regionId) => (tripId,regions(regionId))} toMap
    } yield (tripRegions)
    
    def tripsToUsers(tripIds : Set[String])(implicit ec : ExecutionContext) : DBIO[Map[String,User]] = for {
      tripUserPairs <- (for {
        tr <- slickTrips
        if tr.tripId inSet tripIds
        r = (tr.tripId, tr.userId)
      } yield (r)).result
                     
      userIds = tripUserPairs.map(_._2).toSet
      users <- user.fill(userIds)
      tripUsers = tripUserPairs map { case(tripId,userId) => (tripId,users(userId))} toMap
    } yield (tripUsers)
                             
    type TripToActivityMap[A] = Map[String,Map[Int,Map[Int,A]]]
        
    def tripToActivity[T,A]
        (query : DBIO[Seq[(String,Int,Int,DBActivity,A)]])
         (buildActivity : (DBActivity,A) => T)
        (implicit ec : ExecutionContext) :         
            DBIO[TripToActivityMap[T]] = for {                    
      dbActivities <- query      
      activitiesSeq = dbActivities map { 
          case (a,dn,o,dba,dbv) => (a,(dn,(o,buildActivity(dba,dbv))))               
         }      
      activitiesMap = groupedMap(activitiesSeq).withDefault(Map())
                      .mapValues(x => groupedMap(x).withDefault(Map()).mapValues(groupedMap1(_)))
    } yield (activitiesMap)
               
    def tripsToVisits(tripIds : Set[String])(implicit ec : ExecutionContext) : 
            DBIO[TripToActivityMap[Visit]] = 
      tripToActivity(
          slickActivities
            .join(slickVisitActivities)
            .on({case (x,y) => x.tripId === y.tripId && x.dayNumber === y.dayNumber && x.order === y.order})
            .filter(_._1.tripId inSet tripIds)
            .join(slickPointsOfInterest)
            .on({case ((_,v),p) => v.visitPOIId === p.poiId})
            .map({case ((a,v),p) => (a.tripId, a.dayNumber, a.order, a, (v, p))})
            result)(
          { case (a,(v,p)) => Visit(poi.makeRef(p),v.visitDescription,a.lengthHours) }) 
    
    def tripsToTransports(tripIds : Set[String])(implicit ec : ExecutionContext) : 
            DBIO[TripToActivityMap[Transport]] = 
      tripToActivity(
          slickActivities
            .join(slickTransportActivities)
            .on({case (x,y) => x.tripId === y.tripId && x.dayNumber === y.dayNumber && x.order === y.order})
            .filter(_._1.tripId inSet tripIds)   
            .join(slickCities)
            .on({case ((_,t),c) => t.fromCityId === c.cityId})
            .join(slickCities)
            .on({case (((_,t),_),c) => t.toCityId === c.cityId})
            .join(transportModalities)
            .on({case ((((_,t),_),_),m) => t.transportModalityId === m.transportModalityId})
            .map({case ((((a,t),fc),tc),m) => (a.tripId, a.dayNumber, a.order, a, (t,fc,tc,m))})
            result)(
          { case (dbActivity,(dbTransport,dbFromCity,dbToCity,dbTranspModality)) => Transport(
                  city.makeRef(dbFromCity),
                  city.makeRef(dbToCity),
                  transportModality.makeRef(dbTranspModality),
                  dbTransport.transportDescription,
                  dbActivity.lengthHours) })      
                  
    def tripsToActivities(tripIds : Set[String])(implicit ec : ExecutionContext) : 
            DBIO[TripToActivityMap[Activity]] = for {
      visits <- tripsToVisits(tripIds)
      transports <- tripsToTransports(tripIds)
      dbAllActivitiesSeq <- slickActivities
             .filter(_.tripId inSet tripIds)
             .result
      allActivitiesSeq = dbAllActivitiesSeq
             .map(x => (x.tripId, (x.dayNumber, (x.order,UndefinedActivity(x.lengthHours)))))             
      allActivities = groupedMap(allActivitiesSeq).mapValues( 
                        groupedMap(_).mapValues(
                            groupedMap1(_)))
      mergedMaps = mergeMaps3_F(visits,transports,allActivities) { case (tripId,x,y,z) =>
        mergeMaps3_F(x,y,z) { case (dayNum,visitMap,transpMap,actMap) =>
          mergeMaps3_1(visitMap,transpMap,actMap) 
        }
      } 
    } yield(mergedMaps)
    
      
    
    def tripsToTripDayMaps(tripIds : Set[String])(implicit ec : ExecutionContext) : 
      DBIO[Map[String,SortedMap[Int,TripDay]]] = for {
      activities <- tripsToActivities(tripIds)
      tripDaysSeq <- slickTripDays.filter(_.tripId inSet tripIds).result        
      tripDaysMap = tripDaysSeq.groupBy(_.tripId) map { case (tripId,daySeq) =>        
        val dayMap = activities(tripId)
        val unsortedDayMap = daySeq.groupBy(_.dayNumber) map { case (dayNum,Seq(dbDay)) =>
           val sortedActivities = SortedMap(dayMap(dayNum).toSeq:_*).values.toSeq
           (dayNum,TripDay(dbDay.label,sortedActivities))
        }                 
        (tripId,SortedMap(unsortedDayMap.toSeq : _*))
      }           
    } yield (tripDaysMap)
        
    def fillTrips(ids : Set[String])(implicit ec : ExecutionContext) : DBIO[Map[UserId,Trip]] = for {                  
      tripDays <- tripsToTripDayMaps(ids)
      dbTrips <- 
        slickTrips
          .filter(_.tripId inSet ids)
          .join(slickRegions)
          .on({case (t,r) => t.regionId === r.regionId})
          .join(slickUsers)
          .on({case ((t,_),u) => t.userId === u.id})          
          .join(slickUserLoginInfos)
          .on({case ((_,u),sli) => u.id === sli.userID})
          .join(slickLoginInfos)
          .on({case ((_,sli),li) => sli.loginInfoId === li.id})                    
          .result
          .map(_.map({case ((((t,r),u),_),li) => (t.tripId,(t,r,u,li))})).map(_.toMap)
      tripMap = mergeMaps2_F(dbTrips,tripDays) {
        case (tripId, (dbTrip,dbRegion,dbUser,dbLoginInfo), days) => Trip(
              makeRef(dbTrip),
              userRef = user.makeRef(dbUser,dbLoginInfo),              
              isPublic = dbTrip.tripIsPublic,
              days = days,
              regionRef = region.makeRef(dbRegion)
            )
      }            
    } yield (tripMap.map { case (k,v) => (UUID.fromString(k),v)})
      
    def findPublic()(implicit ec : ExecutionContext): DBIO[Map[TripId,Trip]] = 
      slickTrips.filter(_.tripIsPublic).map(_.tripId).to[Set].result flatMap fillTrips             
    
    def find(tripId : TripId, userId : Option[UserId] = None)(implicit ec : ExecutionContext): DBIO[Option[Trip]] = {
      val userIsNone = userId.isEmpty
      val base = slickTrips.filter(t => t.tripIsPublic).map(_.tripId)
      val withUser = userId.map(u => slickTrips.filter(_.userId === u.toString).map(_.tripId))
                           .getOrElse(base)
      for {
        ids <- withUser.to[Set].result
        tm <- fillTrips(ids)                           
      } yield (tm.values.headOption)        
    }
  
    def exists(tripId : TripId, userId : Option[UserId] = None)(implicit ec : ExecutionContext) : DBIO[Boolean] = {
      (userId match {
        case None => slickTrips.filter(t => t.tripId === tripId.toString && t.tripIsPublic).map(_.tripId)
        case Some(u) => slickTrips.filter(t => t.userId === u.toString && t.tripId === tripId.toString).map(_.tripId)
      }).exists.result
    }
  
    def findByUser(u: UserId)(implicit ec : ExecutionContext): DBIO[Map[TripId,Trip]] = {
      slickTrips.filter(_.userId === u.toString).map(_.tripId).to[Set].result flatMap fillTrips
    }
  
    def save(trip : Trip, userId : Option[UserId] = None)(implicit ec : ExecutionContext) : DBIO[Trip] = {
      val tripId = trip.ref.id.toString
      val dbTrip = DBTrip(
          tripId,trip.ref.name,trip.isPublic,
          trip.userRef.id.toString,trip.regionRef.id)
      val dbTripDays = trip.days.toSeq map {
        case (dayNum,td) => DBTripDay(tripId,dayNum,td.label)          
      }
      val dbActivities = trip.days.toSeq.flatMap {
        case (dayNum,td) => td.activities.zipWithIndex map {
          case (a,order) => DBActivity(tripId,dayNum,order,a.lengthHours)
        }
      }
      val dbVisitActivities = trip.days.toSeq.flatMap {
        case (dayNum,td) => td.activities.zipWithIndex collect {
          case (v : Visit,order) => 
            DBVisitActivity(tripId,dayNum,order,v.poiRef.id,v.description)
        }
      }
      val dbTransportActivities = trip.days.toSeq.flatMap {
        case (dayNum,td) => td.activities.zipWithIndex collect {
          case (t : Transport,order) => 
            DBTransportActivity(
               tripId,dayNum,order,
               t.fromCity.id,
               t.toCity.id,
               t.transportModalityRef.id,
               t.description)
        }
      }                               
      (for {
         tripExists <- slickTrips
                .filter(_.tripId === dbTrip.tripId)
                .exists
                .result  
         _ <- if(tripExists) {
           slickTrips.filter(_.tripId === dbTrip.tripId).update(dbTrip)
         } else {
           slickTrips += dbTrip
         }
         _ <- slickTripDays.filter(_.tripId === dbTrip.tripId).delete
         _ <- slickTripDays ++= dbTripDays
         _ <- slickActivities ++= dbActivities
         _ <- slickVisitActivities ++= dbVisitActivities
         _ <- slickTransportActivities ++= dbTransportActivities
       } yield (trip)).transactionally
    }     
  }
  
  object user extends Logger {                 
    def makeRef(dbUser : DBUser, dbLoginInfo : DBLoginInfo) : Ref[UserId] = 
      dbUserToUser(dbUser,dbLoginInfo).ref
    
    def dbLItoLI(dbli : DBLoginInfo) : LoginInfo = LoginInfo(
        providerID = dbli.providerID,
        providerKey = dbli.providerKey
        )
    
    val dbUserToUser : (DBUser,DBLoginInfo) => User = { case (dbu : DBUser, dbli : DBLoginInfo) => User (
         userID = UUID.fromString(dbu.userID),
         loginInfo = dbLItoLI(dbli),
         firstName = dbu.firstName,
         lastName = dbu.lastName,
         fullName = dbu.fullName,
         email = dbu.email,
         avatarURL = dbu.avatarURL
        )
    }
    
    def fill(userIds : Set[String])(implicit ec : ExecutionContext) : DBIO[Map[String,User]] = for {
      dbUsers <- slickUsers
        .filter(_.id inSet userIds)
        .join(slickUserLoginInfos)
        .on({case (u,uli) => u.id === uli.userID})
        .join(slickLoginInfos)
        .on({case ((_,uli),li) => uli.loginInfoId === li.id})
        .map({case ((u,_),li) => (u,li)})
        .result            
    } yield(dbUsers.map { case (dbu,dbli) => (dbu.userID,dbUserToUser(dbu,dbli)) }.toMap)
      
    
    /**
     * Finds a user by its login info.
     *
     * @param loginInfo The login info of the user to find.
     * @return The found user or None if no user for the given login info could be found.
     */
    def find(loginInfo: LoginInfo)(implicit ec : ExecutionContext) : DBIO[Option[User]] = 
      slickLoginInfos
              .filter(li => li.providerID === loginInfo.providerID &&
                                      li.providerKey === loginInfo.providerKey)
              .join(slickUserLoginInfos)
              .on({case (li,uli) => uli.loginInfoId === li.id})
              .join(slickUsers)
              .on({case ((_,uli),u) => uli.userID === u.id})
              .map({case ((dbli,_),dbu) => (dbu,dbli)})
              .result
              .headOption
              .map(_.map(dbUserToUser.tupled))
              .transactionally
     
  
    /**
     * Finds a user by its user ID.
     *
     * @param userID The ID of the user to find.
     * @return The found user or None if no user for the given ID could be found.
     */
    def find(userID: UUID)(implicit ec : ExecutionContext) : DBIO[Option[User]] =
      fill(Set(userID.toString)).map(_.values.headOption)
      
     

    /**
     * Saves a user.
     *
     * @param user The user to save.
     * @return The saved user.
     */
    def save(user: User)(implicit ec : ExecutionContext) = {
      logger.debug(s"save: user=${user}")
      val dbUser = DBUser(user.userID.toString, user.firstName, user.lastName, user.fullName, user.email, user.avatarURL)
      for {                
        userExists <- slickUsers.filter(_.id === dbUser.userID).exists.result
        _ <- if(userExists) {
          slickUsers.filter(_.id === dbUser.userID).update(dbUser)          
        } else {
          slickUsers += dbUser
        }
        loginInfo = user.loginInfo
        optLoginInfoId <- slickLoginInfos.filter(
                  x => x.providerID === loginInfo.providerID &&
                       x.providerKey === loginInfo.providerKey)
                  .map(_.id).result.headOption
        loginInfoId <- optLoginInfoId match {
          case None => for {
            _ <- slickLoginInfos += 
                   DBLoginInfo(
                     None,
                     loginInfo.providerID,
                     loginInfo.providerKey)
            loginInfoId <- slickLoginInfos.filter(
                  x => x.providerID === loginInfo.providerID &&
                       x.providerKey === loginInfo.providerKey)
                  .map(_.id).result.head
          } yield (loginInfoId)          
          case Some(loginInfoId) => for {
            _ <- slickLoginInfos.filter(_.id === loginInfoId)
                  .update(
                   DBLoginInfo(
                     Some(loginInfoId),
                     loginInfo.providerID,
                     loginInfo.providerKey))        
          } yield (loginInfoId)
        }        
        suliExists <- slickUserLoginInfos.filter(x =>
            x.userID === dbUser.userID && 
            x.loginInfoId === loginInfoId).exists.result
        _ <- if(!suliExists) {
                slickUserLoginInfos += DBUserLoginInfo(dbUser.userID, loginInfoId)
             } else {
               DBIO.successful(())
             }         
      } yield { 
        logger.debug(s"save: ending")
        user 
      }
    } transactionally                  
  }
}

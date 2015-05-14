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

trait Converters {
 
}

 
class SlickQueries @Inject() (dbConfigProvider : DatabaseConfigProvider)  extends Converters {
  import dbobjs._
  object Implicits {
    implicit def dbRegionToRegion(dbRegion : DBRegion) : Region = 
      Region(dbRegion.regionId, dbRegion.regionName,
           dbRegion.regionDescription, dbRegion.regionThumbnail)
           
    implicit def regionToDbRegion(region : Region) : DBRegion = 
      DBRegion(region.regionId, region.regionName,
             region.regionDescription, region.regionThumbnail)
             
    implicit def dbPlaceToPlace(dbPlace : DBPlace) : Place =
      Place(dbPlace.placeId,dbPlace.placeName,dbPlace.placeDescription)
    
    implicit def placeToDBPlace(place : Place) : DBPlace = 
      DBPlace(place.placeId,place.placeName,place.placeDescription)
      
    implicit def dbTranspModToDomain(t : DBTransportModality) =
        TransportModality(t.transportModalityId,t.transportModalityName)
        
    implicit def domainTranspModToDb(t : TransportModality) =
        DBTransportModality(t.transportModalityId.toString,t.transportModalityName)
  }
  
  import Implicits._

  lazy val dbConfig = dbConfigProvider.get[RelationalProfile]
  
  import dbConfig.driver.api._  
  implicit val db = dbConfig.db       
  
  abstract class MapCache[K,V] {
    val ec = scala.concurrent.ExecutionContext.global
    val agent = Agent[Option[Map[K,V]]](None)(ec)
    
    def invalidateCache() = DBIO.from(agent.alter(None))
    
    def buildMap()(implicit ec : ExecutionContext) : DBIO[Map[K,V]] 
    
    def reloadCache()(implicit ec : ExecutionContext) : DBIO[Map[K,V]] = for {       
      _ <- DBIO.successful("reloadCache: starting")
      newCache <- buildMap()
      _ <- DBIO.from (agent.alter(Some(newCache)))
      _ <- DBIO.successful("reloadCache: ending")
    } yield (newCache)      
    
    def apply()(implicit ec : ExecutionContext) : DBIO[Map[K,V]] = (for {        
      _ <- DBIO.successful("apply: starting")
      cache <- agent() match {
        case Some(c) => DBIO.successful(c)
        case None => reloadCache()
      }        
      _ <- DBIO.successful("apply: ending")
    } yield (cache)).transactionally
    
    def update(k : K, v : V)(implicit ec : ExecutionContext) = for {
      _ <- DBIO.successful("update: starting")
       cache <- reloadCache()         
       newCache = cache + (k -> v)
       _ <- DBIO.from(agent.alter(Some(newCache)))           
      _ <- DBIO.successful("update: ending")
    } yield(newCache)
  }
  
  def exists_[T <: AbstractTable[_], R <: Rep[_]](table : TableQuery[T])(f: T => R)(implicit wt: CanBeQueryCondition[R]) = 
      table.filter(f).exists.result
    
  object region {          
                
    val cache = new MapCache[String,Region] {
      override def buildMap()(implicit ec : ExecutionContext) = for {
        dbRegionSeq <- slickRegions            
            .map(r => (r.regionId, r))
            .result
        dbRegionMap = dbRegionSeq.toMap
        regionSubRegionSeq <- slickRegionSubRegions.
             map(x => (x.subRegionId, x.superRegionId)).result
        regionToSuperRegionsMap = groupedMap(regionSubRegionSeq)        
        regions = {
          val regionMap = scala.collection.mutable.Map[RegionId,Region]()
          
          def region(regionId : String, visitedIds : Set[String] = Set()) : (Set[String],Region) = {
            if (visitedIds(regionId)) {
              throw new RuntimeException(s"loop detected in region: ${regionId}")
            } else { 
              regionMap.get(regionId) match {
                case None => 
                  val dbRegion = dbRegionMap(regionId)
                  val baseRegion = dbRegionToRegion(dbRegion)
                  val superRegionIds = regionToSuperRegionsMap(regionId)
                  val (newVisited,superRegs : Seq[Region] )  = superRegionIds.toSeq.foldLeft((visitedIds + regionId,Seq[Region]())) {
                    case ((visited,s),id) => 
                        val (visited1,reg) = region(id)
                        (visited,reg +: s)
                  }
                  val completeRegion = baseRegion.copy(superRegions = superRegs.toSet)
                  regionMap += (regionId -> completeRegion)
                  (newVisited,completeRegion)
                case Some(r) => (visitedIds,r)
              }
             }
          }
          dbRegionMap.keySet.foreach(region(_))
          regionMap.toMap
        }          
      }  yield (regions)       
    }
          
    def fill(ids : Set[String])(implicit ec : ExecutionContext) : DBIO[Map[String,Region]] = for {
       cache <- cache()         
    } yield (cache.filterKeys(ids))
         
    def find(regionId : RegionId)(implicit ec : ExecutionContext) : DBIO[Option[Region]] = for {
      cache <- cache()                
    } yield (cache.get(regionId))
                        
    def exists(regionId : RegionId)(implicit ec : ExecutionContext) : DBIO[Boolean] =
      find(regionId).map(_.isDefined)            
      
    def save(region : Region)(implicit ec : ExecutionContext) = for {
        regionExists <- exists(region.regionId)
        _ <- if (regionExists) { for {
            _ <- slickRegions.filter(_.regionId === region.regionId.toString).update(region)
            _ <- slickRegionSubRegions.filter(_.subRegionId === region.regionId).delete
          } yield ()
        } else {
          slickRegions += region
        }
        _ <- slickRegionSubRegions ++= 
          region.superRegions.map(r => DBRegionSubRegion(r.regionId,region.regionId))
        _ <- cache.invalidateCache()
    } yield (region)
  }
  
  object place {
    val cache = new MapCache[String,Place] {
      override def buildMap()(implicit ec : ExecutionContext) = for {            
        dbPlaceSeq <- slickPlaces            
            .map(r => (r.placeId, r))
            .result
        dbPlaceMap = dbPlaceSeq.toMap
        regionCache <- region.cache()
        dbPlaceRegionSeq <- slickPlaceRegions.map(x => (x.placeId, x.regionId)).result
        dbPlaceRegionMap = 
            groupedMap(dbPlaceRegionSeq).mapValues(
                _.map(regionCache).to[Set]).withDefault(_ => Set[Region]())      
        places = dbPlaceMap.map {
            case (placeId,dbPlace) => 
               (placeId, dbPlaceToPlace(dbPlace)
                         .copy(regions = dbPlaceRegionMap(placeId)))
        }                          
      } yield (places)      
    }            
    
    def fill(placeIds : Set[String])(implicit ec : ExecutionContext) : DBIO[Map[String,Place]] = for {
      placeCache <- cache()        
    } yield (placeCache.filterKeys(placeIds))
    
    def find(placeId : PlaceId)(implicit ec : ExecutionContext) : DBIO[Option[Place]]= for {
      placeCache <- cache()
    } yield (placeCache.get(placeId))        
    
    def exists(placeId : PlaceId)(implicit ec : ExecutionContext) : DBIO[Boolean] = 
      find(placeId).map(_.isDefined)
              
    def save(place : Place)(implicit ec : ExecutionContext) : DBIO[Place] = for {
      placeExists <- exists(place.placeId)
      _ <- if (placeExists) { for {
               _ <-  slickPlaces.filter(_.placeId === place.placeId).update(place)
               _ <-  slickPlaceRegions.filter(_.placeId === place.placeId).delete
             } yield (())               
           } else {
             slickPlaces += place
           }        
      _ <- slickPlaceRegions ++= place.regions.toSeq.map(r => DBPlaceRegion(place.placeId,r.regionId))
      _ <- cache.invalidateCache()
    } yield (place)          
  }
  
  object transportModality {
    
    val cache = new MapCache[String,TransportModality] {
      def buildMap()(implicit ec : ExecutionContext) = for {
        tseq <- transportModalities.map(x => (x.transportModalityId,x)).result
        tmap = groupedMap1(tseq).mapValues(x => x : TransportModality)
      } yield(tmap)
    }
    
    def allModalities()(implicit ec : ExecutionContext) : DBIO[Map[String,TransportModality]] = cache()
    
    def find(transportModalityId : TransportModalityId)(implicit ec : ExecutionContext) : DBIO[Option[TransportModality]]=  
      cache() map (_ get (transportModalityId))
          
    def exists(transportModalityId : TransportModalityId)(implicit ec : ExecutionContext) : DBIO[Boolean] = 
      find(transportModalityId) map (_ isDefined)
                  
    def save(transportModality : TransportModality)(implicit ec : ExecutionContext) : DBIO[TransportModality] = for {
      tmExists <- exists(transportModality.transportModalityId)
      _ <- if(tmExists) { 
             transportModalities
              .filter(_.transportModalityId === transportModality.transportModalityId)
              .update(transportModality)
           } else { 
             transportModalities += transportModality
           }         
      _ <- cache.update(transportModality.transportModalityId,transportModality)
    } yield (transportModality)
    
  }
  
  object trip {                               
    
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
        
    def tripToActivity[T,A,ParamMap]
        (query : DBIO[Seq[(String,Int,Int,DBActivity,A)]])(
         fillParams : Seq[A] => DBIO[ParamMap])(
         buildActivity : (DBActivity,A,ParamMap) => T)
        (implicit ec : ExecutionContext) :         
            DBIO[TripToActivityMap[T]] = for {                    
      dbActivities <- query
      paramsMap <- fillParams(dbActivities.map(_._5))
      activitiesSeq = dbActivities map { 
          case (a,dn,o,dba,dbv) => (a,(dn,(o,buildActivity(dba,dbv,paramsMap))))               
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
            .map({case (a,v) => (a.tripId, a.dayNumber, a.order, a, v)})
            result)( 
          dbVisits => place.fill(dbVisits.map(_.visitPlaceId).toSet))(
          { case (a,v,placesMap) => Visit(placesMap(v.visitPlaceId),v.visitDescription,a.lengthHours) }) 
    
    def tripsToTransports(tripIds : Set[String])(implicit ec : ExecutionContext) : 
            DBIO[TripToActivityMap[Transport]] = 
      tripToActivity(
          slickActivities
            .join(slickTransportActivities)
            .on({case (x,y) => x.tripId === y.tripId && x.dayNumber === y.dayNumber && x.order === y.order})
            .filter(_._1.tripId inSet tripIds)   
            .map({case (a,v) => (a.tripId, a.dayNumber, a.order, a, v)})
            result)( 
          dbValues => for {
            placesMap <- place.fill(dbValues.flatMap(x => Seq(x.fromPlaceId, x.toPlaceId)).toSet)
            modalitiesMap <- transportModality.allModalities 
          } yield (placesMap,modalitiesMap))(
          { case (a,v,(placesMap,modalitiesMap)) => Transport(
                  placesMap(v.fromPlaceId),
                  placesMap(v.toPlaceId),
                  modalitiesMap(v.transportModalityId),
                  "",
                  a.lengthHours) })      
                  
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
      tripRegions <- tripsToRegions(ids)
      tripUsers <- tripsToUsers(ids)
      tripDays <- tripsToTripDayMaps(ids)
      dbTrips <- slickTrips.filter(_.tripId inSet ids).map(x => (x.tripId,x)).result.map(_.toMap)
      tripMap = mergeMaps4_F(dbTrips,tripUsers,tripDays,tripRegions) {
        case (tripId, dbTrip, user, days, region) => Trip(
              tripId = UUID.fromString(tripId),
              user = user,
              tripName = dbTrip.tripName,
              tripIsPublic = dbTrip.tripIsPublic,
              days = days,
              region = region
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
      val tripId = trip.tripId.toString
      val dbTrip = DBTrip(
          tripId,trip.tripName,trip.tripIsPublic,
          trip.user.userID.toString,trip.region.regionId.toString())
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
            DBVisitActivity(tripId,dayNum,order,v.place.placeId,v.visitDescription)
        }
      }
      val dbTransportActivities = trip.days.toSeq.flatMap {
        case (dayNum,td) => td.activities.zipWithIndex collect {
          case (t : Transport,order) => 
            DBTransportActivity(
               tripId,dayNum,order,
               t.fromPlace.placeId,
               t.toPlace.placeId,
               t.transportModality.transportModalityId.toString,
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
  
  object user extends SilhoutteDBTableDefinitions {             
    val cache = new MapCache[String,User] {
      def buildMap()(implicit ec : ExecutionContext) = for {
        _ <- DBIO.successful(println("buildMap starting"))
        userLogins <- (for {
            user <- slickUsers
            uli <- slickUserLoginInfos
            if user.id === uli.userID
            loginInfo <- slickLoginInfos
            if loginInfo.id === uli.loginInfoId            
          } yield(user,loginInfo)).result
        users = userLogins map { case (user,loginInfo) =>
          (user.userID,User(UUID.fromString(user.userID), LoginInfo(loginInfo.providerID, loginInfo.providerKey), user.firstName, user.lastName, user.fullName, user.email, user.avatarURL))
        } toMap
      } yield { println(s"buildMap ending: users=${users}") ; users }
    }
    
    val loginInfoToUserCache = new MapCache[(String,String),String] {
      def buildMap()(implicit ec : ExecutionContext) = { println("loginInfoToUserCache.buildMap started") ; for {
        seq <- slickLoginInfos
              .join(slickUserLoginInfos)
              .on({case (li,uli) => uli.loginInfoId === li.id})
              .map({case (li,uli) => ((li.providerID, li.providerKey), uli.userID)})
              .result
        m = groupedMap1(seq)
        _ <- DBIO.successful(println(s"login info cache: m = ${m}"))
      } yield(m) }
    }
    
    def fill(userIds : Set[String])(implicit ec : ExecutionContext) =
      cache() map (_ filterKeys userIds)
    
    /**
     * Finds a user by its login info.
     *
     * @param loginInfo The login info of the user to find.
     * @return The found user or None if no user for the given login info could be found.
     */
    def find(loginInfo: LoginInfo)(implicit ec : ExecutionContext) = (for {
      loginUserMap <- loginInfoToUserCache()
      userMap <- cache()
    } yield {
      loginUserMap
        .get((loginInfo.providerID,loginInfo.providerKey))
        .map(userMap)
    }) transactionally
     
  
    /**
     * Finds a user by its user ID.
     *
     * @param userID The ID of the user to find.
     * @return The found user or None if no user for the given ID could be found.
     */
    def find(userID: UUID)(implicit ec : ExecutionContext) = (for {
      cache <- cache()
      user = cache.get(userID.toString)
    } yield(user)) transactionally
     

    /**
     * Saves a user.
     *
     * @param user The user to save.
     * @return The saved user.
     */
    def save(user: User)(implicit ec : ExecutionContext) = {
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
        _ <- slickUserLoginInfos.filter(_.userID === dbUser.userID).delete
        _ <- slickUserLoginInfos += DBUserLoginInfo(dbUser.userID, loginInfoId)
        _ <- cache.update(dbUser.userID,user)
        _ <- loginInfoToUserCache.update((loginInfo.providerID,loginInfo.providerKey),dbUser.userID)
      } yield (user)
    } transactionally                  
  }
}

package models.daos.slickdaos

import java.util.UUID
import scala.collection.breakOut
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
import java.net.URL
import utils.MyHelpers


 
class SlickQueries @Inject() (dbConfigProvider : DatabaseConfigProvider)  {
  import dbobjs._
  object Implicits {    
      
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
    
  
  object feature {    
     def makeRef(dbFeature : DBFeature) = Ref(dbFeature.id,dbFeature.name)
    
     def find(featureId : FeatureId)(implicit ec : ExecutionContext) : DBIO[Option[Feature]] = 
       findAll(Set(featureId)).map(_.values.headOption)

     def exists(featureId : FeatureId)(implicit ec : ExecutionContext) : DBIO[Boolean] = {
       features.filter(_.id === featureId).exists.result
     }  
     
     def fillRefs(ids : Seq[FeatureId])(implicit ec: ExecutionContext) : DBIO[Map[FeatureId,Ref[FeatureId]]] = {
       features.filter(_.id inSet ids).map(f => (f.id,f.name)).result
               .map(_.map({ case (id,name) => (id,Ref(id,name)) })(breakOut))
     } 
     
     def fillRef(id : FeatureId)(implicit ec: ExecutionContext) : DBIO[Option[Ref[FeatureId]]] = {
         features.filter(_.id === id)
                 .map(f => f.name)
                 .result
                 .headOption
                 .map(_.map(name => Ref(id,name)))
     }
     
     def findAll(ids : Set[FeatureId])(implicit ec : ExecutionContext) : DBIO[Map[FeatureId,Feature]] = {
       for {
         dbFeatures <- features.filter(_.id inSet ids).result
         parentFeatureMap <- featureHierarchies
                               .join(features)
                               .on({case (h,f) => h.topId === f.id})
                               .filter(_._1.botId inSet ids)
                               .map(r => (r._1.botId,(r._2.id,r._2.name)))                              
                               .result
                               .map(_.groupBy(_._1)
                                     .mapValues(_.map({case (_,(id,name)) => Ref(id,name)}).to[Set])
                                     .withDefaultValue(Set()))      
         childFeatureMap <- featureHierarchies
                               .join(features)
                               .on({case (h,f) => h.botId === f.id})
                               .filter(_._1.topId inSet ids)
                               .map(r => (r._1.topId,(r._2.id,r._2.name)))                              
                               .result
                               .map(_.groupBy(_._1)
                                     .mapValues(_.map({case (_,(id,name)) => Ref(id,name)}).to[Set])
                                     .withDefaultValue(Set()))
         features = dbFeatures.map { dbf =>
             (dbf.id,Feature(
               Ref(dbf.id,dbf.name),
               featureType = FeatureType(dbf.featureType),
               latitude = dbf.latitude,
               longitude = dbf.longitude,
               countryId = dbf.countryId,
               dbPediaResource = dbf.dbpediaResource.map(DBPediaResource),
               wikipediaResource = dbf.wikipediaResource.map(WikipediaResource),
               imageUrl = dbf.imageUrl.map(new URL(_)),
               thumbnailUrl = dbf.thumbnailUrl.map(new URL(_)),
               description = dbf.description,
               superFeatureIds = parentFeatureMap(dbf.id),
               subFeatureIds = childFeatureMap(dbf.id)
             ))
           }.toMap
       } yield (features)
     }
     
     def findRefsByTypeName(featureType : FeatureType,namePart : String)(implicit ec : ExecutionContext) : DBIO[Seq[Ref[FeatureId]]] = {
       val query = namePart
       val q1 = sql"""select id,name from feature where feature_type = ${featureType.typeSymbol.toString} and trigrams_vector(name) @@ trigrams_query(${query.trim}) 
                        order by levenshtein(${namePart},name) limit 20""".as[(Long,String)]
       q1.map(_.map({case (id,name) => Ref(id,name)}))
     }
     
     def findRefsByTypeNameAncestor(featureType : FeatureType,namePart : String, ancestorId : Long)(implicit ec : ExecutionContext) : DBIO[Seq[Ref[FeatureId]]] = {
       println(s"findRefsByTypeNameAncestor: ancestorId=${ancestorId}, namePart='${namePart}', featureType.typeSymbol=${featureType.typeSymbol}")       
       val query = namePart
       val q1 = sql"""
                  select f.id,f.name from feature f join feature_tcl t on f.id = t.id 
                  where t.anc_id=${ancestorId} and
                        feature_type = ${featureType.typeSymbol.toString} and trigrams_vector(name) @@ trigrams_query(${query.trim}) 
                        order by levenshtein(${namePart},name) limit 20""".as[(Long,String)]
       println(s"findRefsByTypeNameAncestor: q1.statements=${q1.statements.mkString("\t","\n\t","\n")}")                              
       q1.map(_.map({case (id,name) => Ref(id,name)}))
     }
  }
      
  object transportModality {
    def makeRef(dbTM : DBTransportModality) : Ref[TransportModalityId] = Ref(dbTM.transportModalityId,dbTM.transportModalityName)
    
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
    
    def tripsToRegions(tripIds : Set[String])(implicit ec : ExecutionContext) : DBIO[Map[String,Feature]] = for {
      tripRegionPairs <- (for {
        tr <- slickTrips
        if tr.tripId inSet tripIds
        r = (tr.tripId, tr.regionId)
      } yield (r)).result
                     
      regionIds = tripRegionPairs.map(_._2).toSet
      regions <- feature.findAll(regionIds)
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
            .join(features)
            .on({case ((_,v),p) => v.visitPOIId === p.id})
            .map({case ((a,v),p) => (a.tripId, a.dayNumber, a.order, a, (v, p))})            
            result)(
          { case (a,(v,p)) => Visit(feature.makeRef(p),a.description,a.lengthHours) }) 
    
    def tripsToTransports(tripIds : Set[String])(implicit ec : ExecutionContext) : 
            DBIO[TripToActivityMap[Transport]] = 
      tripToActivity(
          slickActivities
            .join(slickTransportActivities)
            .on({case (x,y) => x.tripId === y.tripId && x.dayNumber === y.dayNumber && x.order === y.order})
            .filter(_._1.tripId inSet tripIds)   
            .join(features)
            .on({case ((_,t),c) => t.toCityId === c.id})
            .join(transportModalities)
            .on({case (((_,t),_),m) => t.transportModalityId === m.transportModalityId})
            .map({case (((a,t),tc),m) => (a.tripId, a.dayNumber, a.order, a, (t,tc,m))})
            result)(
          { case (dbActivity,(dbTransport,dbToCity,dbTranspModality)) => Transport(                  
                  feature.makeRef(dbToCity),
                  transportModality.makeRef(dbTranspModality),
                  dbActivity.description,
                  dbActivity.lengthHours) })      
                  
    def tripsToActivities(tripIds : Set[String])(implicit ec : ExecutionContext) : 
            DBIO[TripToActivityMap[Activity]] = for {
      visits <- tripsToVisits(tripIds)
      transports <- tripsToTransports(tripIds)
      dbAllActivitiesSeq <- slickActivities
             .filter(_.tripId inSet tripIds)
             .result
      allActivitiesSeq = dbAllActivitiesSeq
             .map(x => (x.tripId, (x.dayNumber, (x.order,UndefinedActivity(x.lengthHours,x.description)))))             
      allActivities = groupedMap(allActivitiesSeq).mapValues( 
                        groupedMap(_).mapValues(
                            groupedMap1(_)))
      mergedMaps = mergeMaps3_F(visits,transports,allActivities) { case (tripId,x,y,z) =>
        mergeMaps3_F(
          x.getOrElse(Map()),
          y.getOrElse(Map()),
          z.getOrElse(Map())) { 
            case (_,visitMap,transpMap,actMap) =>
              mergeMaps3_1(
                  visitMap.getOrElse(Map()),
                  transpMap.getOrElse(Map()),
                  actMap.getOrElse(Map())) 
        }
      } 
    } yield(mergedMaps.withDefaultValue(Map().withDefaultValue(Map())))
    
      
    
    def tripsToTripDayMaps(tripIds : Set[String])(implicit ec : ExecutionContext) : 
      DBIO[Map[String,SortedMap[Int,TripDay]]] = for {
      activities <- tripsToActivities(tripIds)
      tripDaysSeq <- slickTripDays.filter(_.tripId inSet tripIds).result        
      tripDaysMap = tripDaysSeq.groupBy(_.tripId) map { case (tripId,daySeq) =>        
        val dayMap = activities(tripId)
        val unsortedDayMap = daySeq.groupBy(_.dayNumber) map { case (dayNum,Seq(dbDay)) =>
           val sortedActivities = SortedMap(dayMap(dayNum).toSeq:_*).values.to[IndexedSeq]
           (dayNum,TripDay(dbDay.label,sortedActivities))
        }                 
        (tripId,SortedMap(unsortedDayMap.toSeq : _*))
      }           
    } yield (tripDaysMap)

    import MyHelpers._
    
    def fillTrips(ids : Set[String])(implicit ec : ExecutionContext) : DBIO[Map[UserId,Trip]] = for {                  
      tripDays <- tripsToTripDayMaps(ids)
      _ = println(s"fillTrips: ids=${ids}, tripDays=${tripDays}")
      dbTrips <- (for {
        trip <- slickTrips
        if trip.tripId inSet ids
        region <- features
        if region.id === trip.regionId
        city <- features
        if city.id === trip.cityId
        user <- slickUsers
        if trip.userId === user.id
        sli <- slickUserLoginInfos
        if sli.userID === user.id 
        li <- slickLoginInfos
        if sli.loginInfoId === li.id
      } yield (trip,region,city,user,sli,li)).result
        .map(_.map {
          case (t,r,c,u,sli,li) =>
            (t.tripId,(t,r,u,c,sli,li))
        }.toMap)        
      tripMap = mergeMaps2_F(dbTrips,tripDays) {
        case (tripId, Some((dbTrip,dbRegion,dbUser,dbCity,dbUserLoginInfo,dbLoginInfo)), odays) => 
           println(s"fillTrips: odays=${odays}")
           Trip(
              makeRef(dbTrip),              
              description = dbTrip.tripDescription,
              userRef = user.makeRef(dbUser,dbLoginInfo),              
              isPublic = dbTrip.tripIsPublic,
              days = odays.getOrElse(SortedMap()),
              regionRef = feature.makeRef(dbRegion),
              cityRef = feature.makeRef(dbCity)
            )
        case (tripId,tripData,days) => throw new RuntimeException(s"Unexpected state: tripId=${tripId}, tripData=${tripData}, days=${days}")
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
        tm <- fillTrips(ids.filter(_ == tripId.toString))                           
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
  
    def publish(tripId : TripId, userId : UserId)(implicit ec : ExecutionContext) : DBIO[Unit] = {
      slickTrips.filter(_.tripId === tripId.toString).map(r => r.tripIsPublic).update(true).map(_ => ())
    }
    
    def remove(tripId : TripId, userId : UserId)(implicit ec : ExecutionContext) : DBIO[Unit] = {
      slickTrips.filter(_.tripId === tripId.toString).delete.map(_ => ())
    }
    
    def save(trip : Trip, userId : UserId)(implicit ec : ExecutionContext) : DBIO[Trip] = {
      println("-----------")
      println(s"save: trip: ${trip}, userId: ${userId}")
      val tripId = trip.ref.id.toString
      val dbTrip = DBTrip(
          tripId,trip.ref.name,trip.description,
          trip.isPublic,
          trip.userRef.id.toString,trip.regionRef.id,trip.cityRef.id)
      println(s"dbTrip: ${dbTrip}")
      val dbTripDays = trip.days.toSeq map {
        case (dayNum,td) => DBTripDay(tripId,dayNum,td.label)          
      }
      println(s"dbTripDays: ${dbTripDays}")
      val dbActivities = trip.days.toSeq.flatMap {
        case (dayNum,td) => td.activities.zipWithIndex map {
          case (a,order) => DBActivity(tripId,dayNum,order,a.lengthHours,a.description)
        }
      }
      println(s"dbActivities: ${dbActivities}")
      val dbVisitActivities = trip.days.toSeq.flatMap {
        case (dayNum,td) => td.activities.zipWithIndex collect {
          case (v : Visit,order) => 
            DBVisitActivity(tripId,dayNum,order,v.poiRef.id)
        }
      }
      println(s"dbVisitActivities: ${dbVisitActivities}")
      val dbTransportActivities = trip.days.toSeq.flatMap {
        case (dayNum,td) => td.activities.zipWithIndex collect {
          case (t : Transport,order) => 
            DBTransportActivity(
               tripId,dayNum,order,               
               t.toCity.id,
               t.transportModalityRef.id)
        }
      }                               
      println(s"dbTransportActivities: ${dbTransportActivities}")
      (for {
         tripExists <- slickTrips
                .filter(_.tripId === dbTrip.tripId)
                .exists
                .result  
         _ <- if(tripExists) {
           println(s"trip exists: updating to ${dbTrip}")
           slickTrips.filter(_.tripId === dbTrip.tripId).update(dbTrip)
         } else {
           println("trip is new: saving")
           slickTrips += dbTrip
         }
         _ <- slickTripDays.filter(_.tripId === dbTrip.tripId).delete
         _ = println("saving trip days")
         _ <- slickTripDays ++= dbTripDays
         _ = println("saving activities")
         _ <- slickActivities ++= dbActivities
         _ = println("saving visits")
         _ <- slickVisitActivities ++= dbVisitActivities
         _ = println("saving transports")
         _ <- slickTransportActivities ++= dbTransportActivities
         _ = println("transaction finished")
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

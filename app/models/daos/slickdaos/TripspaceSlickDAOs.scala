package models.daos.slickdaos

import models.daos.TripDAO
import com.google.inject.Inject
import models.Trip
import models.UserId
import models.TripId
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import play.api.db.slick.DatabaseConfigProvider
import slick.profile.RelationalProfile
import models.daos.FeatureDAO
import models.Ref
import models.FeatureId
import models.FeatureType

class TripSlickDAO @Inject() (dbConfigProvider : DatabaseConfigProvider, slickQueries : SlickQueries) extends TripDAO {
  lazy val dbConfig = dbConfigProvider.get[RelationalProfile]
  
  import dbConfig.driver.api._
  val db = dbConfig.db
  
  
  def findPublic()(implicit ec : ExecutionContext): Future[Option[Trip]] = ??? 
  
  def find(tripId : TripId, userId : Option[UserId] = None)(implicit ec : ExecutionContext): Future[Option[Trip]] = 
    db.run(slickQueries.trip.find(tripId, userId))  
  
  
  def exists(tripId : TripId, userId : Option[UserId] = None)(implicit ec : ExecutionContext) : Future[Boolean] = ???
  
  def findByUser(userID: UserId)(implicit ec : ExecutionContext): Future[Map[TripId,Trip]] = 
    db.run(slickQueries.trip.findByUser(userID))
  
  def save(trip : Trip, userId : UserId)(implicit ec : ExecutionContext) : Future[Trip] = 
    db.run(slickQueries.trip.save(trip,userId))
    
  def publish(tripId : TripId, userId : UserId)(implicit ec : ExecutionContext) : Future[Unit] = 
    db.run(slickQueries.trip.publish(tripId,userId))
    
  def remove(tripId : TripId, userId : UserId)(implicit ec : ExecutionContext) : Future[Unit] = 
    db.run(slickQueries.trip.remove(tripId,userId))
}

class FeatureSlickDAO @Inject() (dbConfigProvider : DatabaseConfigProvider, slickQueries : SlickQueries) extends FeatureDAO {
  lazy val dbConfig = dbConfigProvider.get[RelationalProfile]
  
  import dbConfig.driver.api._
  val db = dbConfig.db

  def findRefsByTypeName(featureType : FeatureType, namePart : String)(implicit ec : ExecutionContext) : Future[Seq[Ref[FeatureId]]] =
    db.run(slickQueries.feature.findRefsByTypeName(featureType,namePart))
  
  def findRefsByTypeNameAncestor(featureType : FeatureType, namePart : String, ancestorId : Long)(implicit ec : ExecutionContext) : Future[Seq[Ref[FeatureId]]] =
    for {
      res <- db.run(slickQueries.feature.findRefsByTypeNameAncestor(featureType,namePart,ancestorId))
      _ = println(s"findRefsByTypeNameAncestor: res=${res.mkString("\n\t", "\n\t", "\n")}")
    } yield (res)
}
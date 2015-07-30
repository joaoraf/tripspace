package models.services.rdf

import scala.annotation.implicitNotFound
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import models.TripId
import play.api.http.MediaRange

/**
 * @author joao
 */
trait RdfService {
  def acceptedMimeTypes : Set[MediaRange]
  def serializeTrip(tripId : TripId, mimeType : MediaRange,deep : Boolean = false)(implicit ec : ExecutionContext) : Future[Option[String]]
  def serializeTripDay(tripId : TripId, dayNum : Int, mimeType : MediaRange,deep : Boolean = false)(implicit ec : ExecutionContext) : Future[Option[String]]
  def serializeTripActivity(tripId : TripId, dayNum : Int, actOrder : Int, mimeType : MediaRange)(implicit ec : ExecutionContext) : Future[Option[String]]
  def publishRdf(tripId : TripId)(implicit ec : ExecutionContext) : Future[Unit]
}
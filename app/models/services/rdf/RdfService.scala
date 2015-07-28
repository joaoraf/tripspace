package models.services.rdf

import play.api.http.MediaRange

/**
 * @author joao
 */
trait RdfService {
  def serializeTrip(tripId : String, mimeType : MediaRange) : String
  def serializeTripDay(tripId : String, dayNum : Long, mimeType : MediaRange) : String
  def serializeTripActivity(tripId : String, dayNum : Long, mimeType : MediaRange) : String
}
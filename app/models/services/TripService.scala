package models.services

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import forms.TripBaseData
import models._

/**
 * @author joao
 */
trait TripService {
  def tripsForUser(id : UserId)(implicit ec : ExecutionContext) : Future[Seq[Trip]]
  def createTrip(trip : Trip, id : UserId)(implicit ec : ExecutionContext) : Future[Trip]
  def getTrip(tripId : TripId, userId : Option[UserId] = None)(implicit ec : ExecutionContext) : Future[Trip]
  def saveTrip(trip : Trip, userId : UserId)(implicit ec : ExecutionContext) : Future[Trip]
}
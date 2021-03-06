package models.services.impl

import models.services.TripService
import models.UserId
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import models.Trip
import models.daos.TripDAO
import com.google.inject.Inject
import forms.TripBaseData
import models.TripId
import java.util.UUID
import models.Ref
import models.services.rdf.RdfService

/**

 * @author joao
 */
class TripServiceImpl @Inject() (
    tripDao : TripDAO,
    rdfService : RdfService
    ) extends TripService{
    def tripsForUser(id : UserId)(implicit ec : ExecutionContext) : Future[Seq[Trip]] = {
      tripDao.findByUser(id).map(_.values.to[IndexedSeq].sortBy(_.ref.name))
    }
    def createTrip(trip : Trip, id : UserId)(implicit ec : ExecutionContext) : Future[Trip] = {
      tripDao.save(trip, id)
    }
    def getTrip(tripId : TripId, userId : Option[UserId] = None)(implicit ec : ExecutionContext) : Future[Trip] = {
      tripDao.find(tripId, userId).flatMap { 
        case Some(r) => Future.successful(r)
        case _ => Future.failed(new RuntimeException(s"Trip (id: ${tripId}) not found!"))
      }
    }
    
    def saveTrip(trip : Trip, userId : UserId)(implicit ec : ExecutionContext) : Future[Trip] = {
      tripDao.save(trip, userId)
    }
    def publishTrip(tripId : TripId,userId : UserId)(implicit ec : ExecutionContext) : Future[Unit] = {
      tripDao.publish(tripId,userId)      
    }
    def removeTrip(tripId : TripId,userId : UserId)(implicit ec : ExecutionContext) : Future[Unit] = {
      tripDao.remove(tripId,userId)
    }
}
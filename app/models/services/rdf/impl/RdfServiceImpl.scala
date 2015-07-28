package models.services.rdf.impl

import models._
import models.services.rdf.RdfService
import com.hp.hpl.jena.rdf.model.Resource
import com.hp.hpl.jena.rdf.model.Model
import play.api.http.MediaRange


/**
 * @author joao
 */
class RdfServiceImpl extends RdfService {
   def serializeTrip(tripId : String, mimeType : MediaRange) : String = ""
  def serializeTripDay(tripId : String, dayNum : Long, mimeType : MediaRange) : String = ""
  def serializeTripActivity(tripId : String, dayNum : Long, mimeType : MediaRange) : String = ""
}

object RdfServiceImpl {
  val tripSpacePrefix = "https://tripspace.ngrok.io/assets/owl/tripspace1.0.0"
  val gnPrefix = "http://sws.geonames.org/"
  val tripBaseUri = "https://tripspace.ngrok.io/resource/"
  val rdfPrefix = "http://www.w3.org/1999/02/22-rdf-syntax-ns"
  def tripId(trip : Trip) = s"""${tripBaseUri}trip/${trip.ref.id}"""
  def dayId(trip : Trip,dayNum : Int) = s"""${tripBaseUri}day/${trip.ref.id}/${dayNum}"""
  def actId(trip : Trip,dayNum : Int, actOrder : Int) = s"""${tripBaseUri}day/${trip.ref.id}/${dayNum}/${actOrder}"""
  
  
  
  implicit class RichModel(model : Model) {
    def gnFeature(id : Long) = 
      model.createResource(s"""${gnPrefix}/${id}/""")
  
    def tsConcept(id : String) = 
      model.createProperty(tripSpacePrefix,id)
     
  
    val TripConcept = tsConcept("TspaceTrip")
    val ContainedInRegion = tsConcept("containedInRegion")       
    val StartsInCity = tsConcept("startsInCity")
    val EndsInCity = tsConcept("endsInCity")
    val HasDescription = tsConcept("hasDescription")
    val HasDuration = tsConcept("hasDuration")
    val ContainsActivity = tsConcept("containsActivity")
    val VisitsPlace = tsConcept("visitsPlace")
    val ContainsDay = tsConcept("containsDay")
    val ContainedInTrip = tsConcept("containedInTrip")
    val HappensAtDay = tsConcept("happensAtDay")
    val RdfType = model.createProperty(rdfPrefix,"type") 
  
    def tripResource(trip : Trip) : Resource = {  
      val res = model.createResource(tripId(trip))
           .addProperty(RdfType,TripConcept)
           .addProperty(ContainedInRegion,gnFeature(trip.regionRef.id))
           .addProperty(StartsInCity,gnFeature(trip.cityRef.id))
           .addProperty(EndsInCity,gnFeature(trip.endsIn.get.id))
           .addProperty(HasDescription,trip.description)
      val days = trip.days.to[Seq].map {
        case (dn,day) => tripResource(trip,dn+1,day)          
      }
      days foreach { day =>
        res.addProperty(ContainsDay,day)
      }
      res
    }
    
    def tripResource(trip : Trip,dayNum : Int, day : TripDay) : Resource = {
      ???
    }
  }
  
  
  
  
  
 
  
}
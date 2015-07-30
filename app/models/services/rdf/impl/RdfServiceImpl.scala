package models.services.rdf.impl

import models._
import models.services.rdf.RdfService
import com.hp.hpl.jena.rdf.model.Resource
import com.hp.hpl.jena.rdf.model.Model
import play.api.http.MediaRange
import org.apache.commons.io.output.StringBuilderWriter
import models.daos.TripDAO
import com.hp.hpl.jena.rdf.model.ModelFactory
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import com.google.inject.Inject
import play.api.libs.ws.WSClient
import play.api.libs.ws.WSAuthScheme
import play.api.libs.ws.WSResponse


/**
 * @author joao
 */
class RdfServiceImpl @Inject() (
    tripDao : TripDAO,
    ws : WSClient
  ) extends RdfService {
  import RdfServiceImpl._
  
  def acceptedMimeTypes = mimeToLang.keySet.flatMap(MediaRange.parse(_))
  
  def serializeTrip(tripId : TripId, mimeType : MediaRange, deep : Boolean = false)(implicit ec : ExecutionContext) : Future[Option[String]] = {
    tripDao.find(tripId, None) map { otrip =>
      println(s"serializeTrip: otrip=${otrip}")
      otrip.flatMap { trip =>
        println(s"serializeTrip: trip=${trip}")
        val model = ModelFactory.createDefaultModel 
        model.tripResource(trip,deep)      
        model.asString(mimeType)      
    } }
  }
  def serializeTripDay(tripId : TripId, dayNum : Int, mimeType : MediaRange, deep : Boolean = false)(implicit ec : ExecutionContext) : Future[Option[String]] = {
    val res = tripDao.find(tripId, None) map { 
      _.flatMap { 
          _.days.get(dayNum).flatMap { day =>
            val model = ModelFactory.createDefaultModel 
            model.dayResource(tripId,dayNum,day)
            model.asString(mimeType)      
          } 
      } 
    }
    res
  }
  
  def serializeTripActivity(tripId : TripId, dayNum : Int, actOrder : Int, mimeType : MediaRange)(implicit ec : ExecutionContext) : Future[Option[String]] = {
     println(s"serializeTripActivity: starting, tripId=${tripId}, dayNum=${dayNum}, actOrder=${actOrder}")
     val res = tripDao.find(tripId, None) map { otrip =>
        println(s"otrip=${otrip}")
        otrip.flatMap { trip =>         
          println(s"trip=${trip}")
          trip.days.get(dayNum).flatMap { day =>
            println(s"serializeTripActivity: in the middle day=${day}")
            val acts = day.activities
            println(s"   acts.size=${acts.size}, actOrder=${actOrder}")
            if(acts.size >= actOrder) { 
              println("entrou!")
              val act = acts(actOrder - 1)
              val model = ModelFactory.createDefaultModel 
              model.actResource(tripId,dayNum,actOrder,act)
              model.asString(mimeType)       
            } else {
              None
            }
          } 
      } 
    }
    res
  }
  
  def doPublish(trip : Trip)(implicit ec : ExecutionContext) : Future[Unit] = {
    val model = ModelFactory.createDefaultModel 
    model.tripResource(trip,true)
    
    def checkResponse(wsr : WSResponse) : Future[Unit] = {
      println("checkResponse:")
      wsr.allHeaders.foreach { 
        case (name,vals) => 
          val head = s"   {$name}: "
          val indent = head.replaceAll("."," ")
          val valsTxt = vals.mkString("",s"\n${indent}","") 
          println(s"${head}${valsTxt}") 
      }
      println("body:\n" + wsr.body)
      Future.successful(())
    }
    
    model.asString(MediaRange.parse("text/n3").head) match { 
      case Some(rdfText) =>
        ws.url("http://localhost:9099/tripspace/data?default")
                  .withHeaders("Content-Type" -> "text/n3")
                  .withRequestTimeout(10000)
                  .withAuth("admin","7192mSzOJ2qzXyNhzdJc", WSAuthScheme.BASIC)
                  .withMethod("POST")
                  .withBody(rdfText)
                  .execute()
                  .map(checkResponse)    
      case None =>
          println("rdf not generated!")
          Future.successful(())
          
    }            
    
  }
  
  def publishRdf(tripId : TripId)(implicit ec : ExecutionContext) : Future[Unit] = for {
    otrip <- tripDao.find(tripId, None)
    _ <- otrip match {
            case None => Future.successful(())
            case Some(trip) => doPublish(trip)
         }
    } yield (())      
}

object RdfServiceImpl {
  val tripSpacePrefix = "https://tripspace.ngrok.io/assets/owl/tripspace-1.0.0#"
  val gnPrefix = "http://sws.geonames.org/"
  val tripBaseUri = "https://tripspace.ngrok.io/resource/"
  val rdfPrefix = "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
  def tripName(tripId : TripId) = s"""${tripBaseUri}trip/${tripId}"""
  def dayName(tripId : TripId,dayNum : Int) = s"""${tripBaseUri}day/${tripId}/${dayNum}"""
  def actName(tripId : TripId,dayNum : Int, actOrder : Int) = s"""${tripBaseUri}activity/${tripId}/${dayNum}/${actOrder}"""
  
  val mimeToLang : Map[String,String] = Map(
        "application/rdf+xml" -> "RDF/XML",
        "text/plain" -> "N-TRIPLE",
        "text/turtle" -> "TURTLE",
        "application/x-turtle" -> "TURTLE",
        "text/n3" -> "N3"
    );
  
  
  
  implicit class RichModel(model : Model) {
    private def gnFeature(id : Long) = 
      model.createResource(s"""${gnPrefix}/${id}/""")
  
    private def tsConcept(id : String) = 
      model.createProperty(tripSpacePrefix,id)     
  
    lazy val TripConcept = tsConcept("TspaceTrip")
    lazy val TripDayConcept = tsConcept("TspaceTripDay")
    lazy val TripVisitConcept = tsConcept("TspaceTripVisit")
    lazy val TripTransportConcept = tsConcept("TspaceTripTransport")
    lazy val TripFreeTimeConcept = tsConcept("TspaceTripFreeTime")
    lazy val ContainedInRegion = tsConcept("containedInRegion")       
    lazy val StartsInCity = tsConcept("startsInCity")
    lazy val EndsInCity = tsConcept("endsInCity")
    lazy val HasDescription = tsConcept("hasDescription")
    lazy val HasDuration = tsConcept("hasDuration")
    lazy val ContainsActivity = tsConcept("containsActivity")
    lazy val VisitsPlace = tsConcept("visitsPlace")
    lazy val ContainsDay = tsConcept("containsDay")
    lazy val ContainedInTrip = tsConcept("containedInTrip")
    lazy val HappensAtDay = tsConcept("happensAtDay")
    lazy val SequenceNumber = tsConcept("sequenceNumber")
    lazy val TransportModality = tsConcept("transportModality")
    lazy val RdfType = model.createProperty(rdfPrefix,"type") 
  
    def tripResource(trip : Trip,deep : Boolean = false) : Resource = {  
      val res = model.createResource(tripName(trip.ref.id))
           .addProperty(RdfType,TripConcept)
           .addProperty(ContainedInRegion,gnFeature(trip.regionRef.id))
           .addProperty(StartsInCity,gnFeature(trip.cityRef.id))
           .addProperty(EndsInCity,gnFeature(trip.endsIn.get.id))
           .addProperty(HasDescription,trip.description)
     
      trip.days.to[Seq] foreach {
          case (dn,day) => 
              val dr = model.createResource(dayName(trip.ref.id,dn))
              res.addProperty(ContainsDay,dr)
      }
      
      if(deep) {
        trip.days.foreach { case (dn,d) => model.dayResource(trip.ref.id,dn,d,true) }
      }
      
      res
    }
    
    def dayResource(tripId : TripId,dayNum : Int, day : TripDay,deep : Boolean = false) : Resource = {
      val res = model.createResource(dayName(tripId,dayNum))
                  .addProperty(RdfType,TripDayConcept)
                  .addProperty(ContainedInTrip,tripName(tripId))
                  .addProperty(SequenceNumber,dayNum.toString)
      day.label foreach { l => res.addProperty(HasDescription,l) }
      day.activities.zipWithIndex foreach {
        case (act,idx) =>
          val ar = model.createResource(actName(tripId,dayNum,idx+1))
          res.addProperty(ContainsActivity,ar)
      }           
      if(deep) {
        day.activities.zipWithIndex foreach { case (a,idx) => model.actResource(tripId,dayNum,idx+1,a) }
      }
      res
    }
    
    def actResource(tripId : TripId,dayNum : Int, actOrder : Int, act : Activity) : Resource = {
      val res = model.createResource(actName(tripId,dayNum,actOrder))
                     .addProperty(HappensAtDay,model.createResource(dayName(tripId,dayNum)))
                     .addProperty(SequenceNumber,actOrder.toString)
                     .addProperty(HasDescription,act.description)
                     .addProperty(HasDuration,act.lengthHours.toString)
      act match {
        case _ : UndefinedActivity => res.addProperty(RdfType,TripFreeTimeConcept)
        case v : Visit => 
              res.addProperty(RdfType,TripVisitConcept)
                 .addProperty(VisitsPlace,gnFeature(v.poiRef.id))            
        case t : Transport => 
              res.addProperty(RdfType,TripTransportConcept)
                 .addProperty(EndsInCity,gnFeature(t.toCity.id))
                 .addProperty(TransportModality,t.transportModalityRef.id)
      }
      res
    }
    
    
    
    def asString(mm : MediaRange,baseUri : Option[String] = None) : Option[String] = {      
      mimeToLang.filterKeys(mm.accepts).headOption map {
        case (mt,lang) =>
          val sw = new StringBuilderWriter()
          model.write(sw,lang,baseUri.orNull)
          sw.close()
          sw.getBuilder.toString()
      }       
    }
  }
  
  
  
  
  
 
  
}
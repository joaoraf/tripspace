package models.services.rdf

object RdfSerialization {
  import models._
  
  def gn(id : Long) : String = {
    s"gn:${id}"
  }

  
  def serialize(trip : Trip) : String = {
    s"""
:${trip.ref.id} a ts:TspaceTrip ;
                ts:containedInRegion ${gn(trip.regionRef.id)} ;
                ts:startsInCity ${gn(trip.cityRef.id)} ;
                ts:endsInCity ${gn(trip.endsIn.get.id)} ;
"""
  }
  
}
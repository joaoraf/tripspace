package object models {
  type ResourceId = String
  type UserId = java.util.UUID
  
  type UniqueId = java.util.UUID
  
  type TripId = UniqueId
  type ScheduleId = UniqueId
  type DayId = UniqueId
  type VisitId = UniqueId
  type TransportId = UniqueId
  type TransportModalityId = UniqueId
  
  type PlaceId = ResourceId
  type RegionId = ResourceId
}
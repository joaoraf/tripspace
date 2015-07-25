package utils

import scala.collection.SortedMap
package object collections {
  def mergeMaps2[K,V1,V2,V](m1 : Map[K,V1], m2 : Map[K,V2])(f : (K,Option[V1],Option[V2]) => V) : Map[K,V] =
    (m1.keySet ++ m2.keySet).toSeq.map(k => (k,f (k,m1.get(k), m2.get(k)))).toMap
    
  def mergeMaps3[K,V1,V2,V3,V](m1 : Map[K,V1], m2 : Map[K,V2], m3 : Map[K,V3])(f : (K,Option[V1],Option[V2],Option[V3]) => V) : 
          Map[K,V] =
    (m1.keySet ++ m2.keySet ++ m3.keySet).toSeq.map(k => (k,f(k,m1.get(k), m2.get(k), m3.get(k)))).toMap
  
  def mergeMaps3_1[K,V](m1 : Map[K,V], m2 : Map[K,V], m3 : Map[K,V]) : Map[K,V] = 
    (m1.keySet ++ m2.keySet ++ m3.keySet).toSeq.map(k => (k,m1.get(k).orElse(m2.get(k)).getOrElse(m3(k)))).toMap
    
  def mergeMaps2_F[K,V1,V2,V](m1 : Map[K,V1], m2 : Map[K,V2])(f : (K,Option[V1],Option[V2]) => V) : Map[K,V] =
    (m1.keySet ++ m2.keySet).toSeq.map(k => (k,f (k,m1.get(k), m2.get(k)))).toMap
  
  def mergeMaps3_F[K,V1,V2,V3,V](m1 : Map[K,V1], m2 : Map[K,V2], m3 : Map[K,V3])(f : (K,Option[V1],Option[V2],Option[V3]) => V) : 
          Map[K,V] =
    (m1.keySet ++ m2.keySet ++ m3.keySet).toSeq.map(k => (k,f(k,m1.get(k), m2.get(k), m3.get(k)))).toMap
    
  def mergeMaps4_F[K,V1,V2,V3,V4,V](m1 : Map[K,V1], m2 : Map[K,V2], m3 : Map[K,V3], m4 : Map[K,V4])(f : (K,Option[V1],Option[V2],Option[V3],Option[V4]) => V) : 
          Map[K,V] =
    (m1.keySet ++ m2.keySet ++ m3.keySet ++ m4.keySet).toSeq.map(k => (k,f(k,m1.get(k), m2.get(k), m3.get(k), m4.get(k)))).toMap
    
  implicit class ToPairMap[A,B](x : Iterable[(A,B)]) {
      def groupPairs : Map[A,Set[B]] = 
        x.groupBy(_._1).mapValues(_.map(_._2).toSet)      
    }
  
  def groupedMap[K,V] : Seq[(K,V)] => Map[K,Seq[V]] = s => 
        s.groupBy(_._1).mapValues(_.map(_._2))
        
  def groupedMap1[K,V] : Seq[(K,V)] => Map[K,V] = s => 
      s.groupBy(_._1).mapValues(_.map(_._2).head)
      
  def groupedSortedMap[K,V](s : Seq[(K,V)])(implicit ordering : scala.Ordering[K]) : SortedMap[K,Seq[V]] = 
      SortedMap(s.groupBy(_._1).toSeq : _*).mapValues(_.map(_._2))
      
  def groupedSortedMap1[K,V](s : Seq[(K,V)])(implicit ordering : scala.Ordering[K]) : SortedMap[K,V] =  
      SortedMap(s.groupBy(_._1).toSeq : _*).mapValues(_.map(_._2).head)
}
package utils

import views.html.bootstrap.bootstrapInlineFieldConstructorTemplate
import java.text.Normalizer

/**
 * @author joao
 */
object MyHelpers {
  object BootstrapInline {
    import views.html.helper.FieldConstructor
    implicit val inlineFields = FieldConstructor(bootstrapInlineFieldConstructorTemplate.f)
  }
  
  
  abstract class FlattenTuple[T1,T2] {
    type F = T1
    type T = T2
    def flatten : F => T
  }
  
  def flattenTuple[T1,T2](x : T1)(implicit ev: FlattenTuple[T1,T2]) =
      ev.flatten(x)
  
  
  implicit def twoToThree[A,B,C] = new FlattenTuple[((A,B),C),(A,B,C)] {
    override def flatten  = {
      case ((x,y),z) => (x,y,z)
    }
  }
  
  implicit def threeToFour[A,B,C,D] = new FlattenTuple[((A,B,C),D),(A,B,C,D)] {
    override def flatten  = {
      case ((x1,x2,x3),x4) => (x1,x2,x3,x4)
    }
  }
  
  implicit def fourToFive[A,B,C,D,E] = new FlattenTuple[((A,B,C,D),E),(A,B,C,D,E)] {
    override def flatten  = {
      case ((x1,x2,x3,x4),x5) => (x1,x2,x3,x4,x5)
    }
  }
  
  implicit def fiveToSix[A,B,C,D,E,F] = new FlattenTuple[((A,B,C,D,E),F),(A,B,C,D,E,F)] {
    override def flatten  = {
      case ((x1,x2,x3,x4,x5),x6) => (x1,x2,x3,x4,x5,x6)
    }
  }
  
  implicit class StringExt(s : String) {
    def unaccent =
      Normalizer
        .normalize(s, Normalizer.Form.NFD)
        .replaceAll("[^\\p{ASCII}]", "")  
  }
}
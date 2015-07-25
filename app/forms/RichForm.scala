package forms

import play.api.data._
import play.api.data.format.Formats._

object FormTools {
  implicit class RichForm[T](form : Form[T]) {
    def extract[T1](prefix : String, mapping : Mapping[T1]) = 
      Form[T1](mapping.withPrefix(prefix.toString)).bind(form.data)    
  }    
}
package forms

import play.api._
import play.api.data._
import play.api.data.Forms._
import models.SearchData

object SearchForm {
  val form = Form(
      mapping(
          "searchString" -> text          
  )(SearchData.apply)(SearchData.unapply)
 )
}
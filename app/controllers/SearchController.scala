package controllers

import com.mohiva.play.silhouette.api.Silhouette

import com.google.inject.Inject
import play.api.i18n.MessagesApi
import play.api.Logger
import com.mohiva.play.silhouette.impl.authenticators.SessionAuthenticator
import models.User
import play.api.mvc.Action
import forms.SearchForm
import com.mohiva.play.silhouette.api.Environment
import models.Ref
import play.api.libs.json._
import models.services.SearchService

/**
 * @author joao
 */
class SearchController @Inject() (  
  messagesApi : MessagesApi,
  searchService : SearchService,
  protected val env: Environment[User, SessionAuthenticator]
  )
  extends Silhouette[User, SessionAuthenticator] {
  
  val search = Action(parse.form(SearchForm.form)) { implicit request =>
    val searchData = request.body
    
    Ok("Search ok: " + searchData)
  }
  
  def searchRegionByName(namePart : String) = SecuredAction.async { implicit request =>
    import request.ec
    searchService.searchRegionByNamePart(namePart) map {
      result => Ok(Json.toJson(result))
    }
  }
  
  def searchCityByName(namePart : String) = SecuredAction.async { implicit request =>
    import request.ec
    searchService.searchCityByNamePart(namePart) map {
      result => Ok(Json.toJson(result))
    }
  }
  
  def searchPlaceByName(namePart : String) = SecuredAction.async { implicit request =>
    import request.ec
    searchService.searchPlaceByNamePart(namePart) map {
      result => Ok(Json.toJson(result))
    }
  }
}
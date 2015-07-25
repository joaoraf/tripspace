package controllers

import java.util.UUID
import scala.concurrent.Future
import com.mohiva.play.silhouette.api.Environment
import com.mohiva.play.silhouette.api.Environment
import com.mohiva.play.silhouette.api.EventBus
import com.mohiva.play.silhouette.api.Logger
import com.mohiva.play.silhouette.api.LogoutEvent
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.services._
import com.mohiva.play.silhouette.impl.authenticators._
import com.mohiva.play.silhouette.impl.authenticators.SessionAuthenticator
import com.mohiva.play.silhouette.impl.daos.DelegableAuthInfoDAO
import com.mohiva.play.silhouette.impl.providers._
import com.mohiva.play.silhouette.impl.providers.oauth2.state.DummyStateProvider
import com.mohiva.play.silhouette.impl.providers.openid.services.PlayOpenIDService
import forms._
import javax.inject.Inject
import models.User
import models.services.TripService
import play.api.i18n.Messages
import play.api.i18n.MessagesApi
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.Result
import models.TripId
import models.TripDay
import models.UndefinedActivity
import play.api.routing.Router
import play.api.routing.JavaScriptReverseRouter

/**
 * The basic application controller.
 *
 * @param socialProviderRegistry The social provider registry.
 * @param env The Silhouette environment.
 */
class ApplicationController @Inject() (
  socialProviderRegistry: SocialProviderRegistry,
  messagesApi : MessagesApi,
  protected val env: Environment[User, SessionAuthenticator],
  tripService : TripService)
  extends Silhouette[User, SessionAuthenticator] with Logger {
  
  
  /**
   * Handles the index action.
   *
   * @return The result to display.
   */
  def index = SecuredAction.async { implicit request =>
    import request.ec
    for {
      trips <- tripService.tripsForUser(request.identity.ref.id)      
    } yield {
      Ok(views.html.index(request.identity,trips))
    }
  }
  
  /**
   * Add a new Trip
   */
  def addTrip = SecuredAction.async { implicit request =>
    Future.successful(Ok(views.html.addTripForm(request.identity)))   
  }
  
  /**
   * create a new trip
   */
  
  def createTrip = SecuredAction.async { implicit request =>   
    import request.ec
    forms.TripBaseData.form().bindFromRequest.fold(
        formWithErrors => {
          println(s"form with errors: ${formWithErrors}")
          Future.successful(BadRequest(views.html.addTripForm(request.identity,formWithErrors)))
        },
        tripBaseData => {
            println(s"tripBaseData: ${tripBaseData}")
            tripService.createTrip(tripBaseData.toTrip(request.identity.userID), request.identity.userID).map {
              trip => Redirect(routes.ApplicationController.editTrip(trip.ref.id.toString))
            }
        }
        )
  }
  
  def editTrip(tripId : String) : Action[AnyContent] = editTrip(UUID.fromString(tripId))
  
  def editTrip(tripId : TripId) : Action[AnyContent] = SecuredAction.async { implicit request  => 
    import request.ec
    tripService.getTrip(tripId, Some(request.identity.userID)) map { trip =>
      println(s"Found trip: ${trip}")
      Ok(views.html.tripEditView(tripId.toString,trip,request.identity))
    }        
  }
  
  def saveTrip(tripId : String) : Action[AnyContent] = saveTrip(UUID.fromString(tripId))
  
  def saveTrip(tripId : TripId) : Action[AnyContent] = SecuredAction.async { implicit request =>
    val form = TripEditData.form().bindFromRequest()
    println(s"form=${form}, errors=${form.errors}")
    
    val ted = form.value.get
    val trip = ted.toTrip(tripId,request.identity.userID)
    import request.ec
    tripService.saveTrip(trip, request.identity.userID) map { _ =>
      Redirect(routes.ApplicationController.viewTrip(trip.ref.id.toString))
    }
  }
  
  def viewTrip(tripId : String) : Action[AnyContent] = viewTrip(UUID.fromString(tripId))
  
  def viewTrip(tripId : TripId) : Action[AnyContent] = SecuredAction.async { implicit request =>
    import request.ec
    tripService.getTrip(tripId, Some(request.identity.userID)) map { trip =>
      val editable = trip.isPublic || trip.userRef.id == request.identity.userID
      Ok(views.html.viewTrip(trip,Some(request.identity),editable))
    }
    
  }

  /**
   * Handles the Sign In action.
   *
   * @return The result to display.
   */
  def signIn = UserAwareAction.async { implicit request =>
    request.identity match {
      case Some(user) => Future.successful(Redirect(routes.ApplicationController.index()))
      case None => Future.successful(Ok(views.html.signIn(SignInForm.form, socialProviderRegistry)))
    }
  }

  /**
   * Handles the Sign Up action.
   *
   * @return The result to display.
   */
  def signUp = UserAwareAction.async { implicit request =>    
    request.identity match {
      case Some(user) => Future.successful(Redirect(routes.ApplicationController.index()))
      case None => Future.successful(Ok(views.html.signUp(SignUpForm.form)))
    }
  }

  /**
   * Handles the Sign Out action.
   *
   * @return The result to display.
   */
  def signOut = SecuredAction.async { implicit request =>
    val messages = messagesApi.preferred(request)
    val result = Redirect(routes.ApplicationController.index())
    env.eventBus.publish(LogoutEvent(request.identity, request, messages))
    import request.ec
    env.authenticatorService.discard(request.authenticator, result)
  }
  
  lazy val javascriptRoutes : Action[AnyContent] = Action { implicit request =>
  Ok(
    JavaScriptReverseRouter("jsRoutes")(
      routes.javascript.SearchController.searchRegionByName
    )
  ).as("text/javascript")
}
}

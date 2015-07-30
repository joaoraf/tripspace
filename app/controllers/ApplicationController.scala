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
import models.JsonTrip
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
import play.api.libs.json.Json
import models.Trip
import models.Ref
import play.api.http.MediaRange
import models.services.rdf.RdfService
import play.api.mvc.Request


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
  tripService : TripService,
  rdfService : RdfService)
  extends Silhouette[User, SessionAuthenticator] with Logger {
  
  
  private def tripsForUser(implicit request : SecuredRequest[AnyContent]) = {
    import request.ec
    for {
      trips <- tripService.tripsForUser(request.identity.ref.id)      
    } yield {
      Ok(views.html.index(request.identity,trips))
    }
  }
  /**
   * Handles the index action.
   *
   * @return The result to display.
   */
  def index = SecuredAction.async { implicit request => tripsForUser }
    
  
  /**
   * Add a new Trip
   */
  def addTrip = SecuredAction.async { implicit request =>
    val newTripId = UUID.randomUUID()
    val user = request.identity
    val userRef = Ref(user.userID,user.name)
    val emptyFeatureRef = Ref[Long](-1,"")
    val newTrip = Trip(ref = Ref(newTripId,""),userRef = userRef, regionRef = emptyFeatureRef, cityRef = emptyFeatureRef )
    Future.successful(Ok(views.html.tripEditView(newTripId.toString,newTrip,request.identity,true)))
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
      println(s"\n\n--------------\nFound trip: ${trip}\n---------------\n\n")
      Ok(views.html.tripEditView(tripId.toString,trip,request.identity))
    }        
  }
  
  /*
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
  * 
  */
  
  def saveTrip() : Action[AnyContent] = SecuredAction.async { implicit request =>
    import JsonTrip._    
    request.body.asJson match { 
      case Some(jsonData) =>
        val repTrip = jsonData.as[RepTrip]
        val trip = repTrip.toTrip
        println(s"saveTrip: jsonData=${jsonData},\n repTrip=${repTrip},\n trip = ${trip}")
        import request.ec
        tripService.saveTrip(trip, request.identity.userID).flatMap { 
          newTrip  => Future.successful(Ok(Json.toJson(RepTrip.fromTrip(newTrip)))); 
        }   
      case None => Future.successful(Ok(""))
    }
    Future.successful(Ok(""))    
  }
  
  def viewTrip(tripId : String) : Action[AnyContent] = viewTrip(UUID.fromString(tripId))
  
  def viewTrip(tripId : TripId) : Action[AnyContent] = SecuredAction.async { implicit request =>
    import request.ec
    tripService.getTrip(tripId, Some(request.identity.userID)) map { trip =>
      val editable = !trip.isPublic && trip.userRef.id == request.identity.userID
      Ok(views.html.viewTrip(trip,Some(request.identity),editable))
    }
    
  }
  
  def publishTrip(tripId : String) : Action[AnyContent] = publishTrip(UUID.fromString(tripId))
  
  def publishTrip(tripId : TripId) : Action[AnyContent] = SecuredAction.async { implicit request =>
      import request.ec
      for {
        _ <- tripService.publishTrip(tripId,request.identity.userID)
        _ <- rdfService.publishRdf(tripId)        
        res <- tripsForUser
      } yield res             
  }
  
  def removeTrip(tripId : String) : Action[AnyContent] = removeTrip(UUID.fromString(tripId))
  
  def removeTrip(tripId : TripId) : Action[AnyContent] = SecuredAction.async { implicit request =>
      import request.ec
      tripService.removeTrip(tripId,request.identity.userID).flatMap(
         _ => index(request))    
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
  
  
  def getRdfMediaRange(request : Request[_]) : Option[MediaRange] =
    if(request.acceptedTypes.exists(rdfService.acceptedMimeTypes)) {
        Some(request.acceptedTypes.filter(rdfService.acceptedMimeTypes).head)
    } else { None }
      
  def withRdfMediaRange(request : Request[_])(f : MediaRange => Future[Option[String]]) = {
      import scala.concurrent.ExecutionContext.Implicits.global
      println("withRdfMediaRange: " + request.acceptedTypes)
      getRdfMediaRange(request) match {
        case Some(mr) =>
            println("withRdfMediaRange: mr = " + mr)
            f(mr).map(x => Ok(x.getOrElse("")))
        case None =>
          f(MediaRange.parse("text/turtle").head).map(x => Ok(x.getOrElse("")))
          //Future.successful(UnsupportedMediaType)
      }
  }
      
  def resourceTrip(tripId : String) : Action[AnyContent] = resourceTrip(UUID.fromString(tripId));
  
  def resourceTrip(tripId : TripId) = Action.async { implicit request =>
      import scala.concurrent.ExecutionContext.Implicits.global
      withRdfMediaRange(request) { 
        mr => rdfService.serializeTrip(tripId,mr) }                           
    }
  
  def resourceDay(tripId : String, dayNum : Int) : Action[AnyContent] = resourceDay(UUID.fromString(tripId), dayNum)
  def resourceDay(tripId : TripId, dayNum : Int) : Action[AnyContent] = Action.async { implicit request =>
    import scala.concurrent.ExecutionContext.Implicits.global
      println(s"resourceDay: ${tripId}, ${dayNum}")
      withRdfMediaRange(request) { mr => println(mr) ; rdfService.serializeTripDay(tripId,dayNum,mr) }          
    }
  
  def resourceActivity(tripId : String, dayNum : Int, order : Int) : Action[AnyContent] =
        resourceActivity(UUID.fromString(tripId), dayNum, order)
  def resourceActivity(tripId : TripId, dayNum : Int, order : Int) : Action[AnyContent] = 
    Action.async { implicit request =>
    println(s"resourceActivity: ${tripId}, ${dayNum}, ${order}")
    import scala.concurrent.ExecutionContext.Implicits.global
    withRdfMediaRange(request) { mr => println(mr); rdfService.serializeTripActivity(tripId,dayNum,order,mr) }    
  }
  
  lazy val javascriptRoutes : Action[AnyContent] = Action { implicit request =>
  Ok(
    JavaScriptReverseRouter("jsRoutes")(
      routes.javascript.SearchController.searchRegionByName,
      routes.javascript.SearchController.searchCityByName,
      routes.javascript.SearchController.searchPlaceByName,
      routes.javascript.ApplicationController.saveTrip,
      routes.javascript.ApplicationController.index,
      routes.javascript.ApplicationController.publishTrip
    )
  ).as("text/javascript")
}
}

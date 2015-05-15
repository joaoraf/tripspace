package controllers

import javax.inject.Inject
import com.mohiva.play.silhouette.api.{ Environment, LogoutEvent, Silhouette }
import com.mohiva.play.silhouette.impl.authenticators.SessionAuthenticator
import com.mohiva.play.silhouette.api.services._
import com.mohiva.play.silhouette.api.util._
import com.mohiva.play.silhouette.api.{ Environment, EventBus }
import com.mohiva.play.silhouette.impl.authenticators._
import com.mohiva.play.silhouette.impl.daos.DelegableAuthInfoDAO
import com.mohiva.play.silhouette.impl.providers._
import com.mohiva.play.silhouette.impl.providers.oauth1._
import com.mohiva.play.silhouette.impl.providers.oauth1.secrets.{ CookieSecretSettings, CookieSecretProvider }
import com.mohiva.play.silhouette.impl.providers.oauth1.services.PlayOAuth1Service
import com.mohiva.play.silhouette.impl.providers.oauth2._
import com.mohiva.play.silhouette.impl.providers.oauth2.state.{ DummyStateProvider, CookieStateProvider, CookieStateSettings }
import com.mohiva.play.silhouette.impl.providers.openid.YahooProvider
import com.mohiva.play.silhouette.impl.providers.openid.services.PlayOpenIDService
import com.mohiva.play.silhouette.impl.services._
import com.mohiva.play.silhouette.impl.util._
import forms._
import models.User
import scala.concurrent.Future
import play.api.i18n.Messages
import play.api.i18n.MessagesApi
import play.api.db.slick.DatabaseConfigProvider
import slick.profile.RelationalProfile

/**
 * The basic application controller.
 *
 * @param socialProviderRegistry The social provider registry.
 * @param env The Silhouette environment.
 */
class ApplicationController @Inject() (
  socialProviderRegistry: SocialProviderRegistry,
  messagesApi : MessagesApi,
  dbConfigProvider : DatabaseConfigProvider,
  protected val env: Environment[User, SessionAuthenticator])
  extends Silhouette[User, SessionAuthenticator] {

  val dbConfig = dbConfigProvider.get[RelationalProfile]
  
  /**
   * Handles the index action.
   *
   * @return The result to display.
   */
  def index = SecuredAction.async { implicit request =>
    Future.successful(Ok(views.html.index(request.identity)))
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
}

package controllers

import javax.inject.Inject
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.impl.authenticators.SessionAuthenticator
import com.mohiva.play.silhouette.impl.exceptions.IdentityNotFoundException
import com.mohiva.play.silhouette.impl.providers._
import forms.SignInForm
import models.User
import models.services.UserService
import play.api.i18n.Messages
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.Action
import scala.concurrent.Future
import play.api.i18n.MessagesApi

/**
 * The credentials auth controller.
 *
 * @param userService The user service implementation.
 * @param AuthInfoRepository The auth info service implementation.
 * @param credentialsProvider The credentials provider.
 * @param socialProviderRegistry The social provider registry.
 * @param env The Silhouette environment.
 */
class CredentialsAuthController @Inject() (
  userService: UserService,
  //authInfoRepository: AuthInfoRepository,
  credentialsProvider: CredentialsProvider,
  socialProviderRegistry: SocialProviderRegistry,
  messagesApi : MessagesApi,
  protected val env: Environment[User, SessionAuthenticator])
  extends Silhouette[User, SessionAuthenticator] {

  /**
   * Authenticates a user against the credentials provider.
   *
   * @return The result to display.
   */
  def authenticate = Action.async { implicit request =>
    val messages = messagesApi.preferred(request)
    SignInForm.form.bindFromRequest.fold(
      form => Future.successful(BadRequest(views.html.signIn(form, socialProviderRegistry))),
      credentials => credentialsProvider.authenticate(credentials).flatMap { loginInfo =>
        val result = Redirect(routes.ApplicationController.index())
        userService.retrieve(loginInfo).flatMap {
          case Some(user) =>
              env.authenticatorService.create(loginInfo).flatMap { authenticator =>
              env.eventBus.publish(LoginEvent(user, request, messages))
              env.authenticatorService.init(authenticator).flatMap { v =>
                env.authenticatorService.embed(v, result)
              }
            }
          case None => Future.failed(new IdentityNotFoundException("Couldn't find user"))
        }
      }.recover {
        case e: ProviderException =>
          Redirect(routes.ApplicationController.signIn()).flashing("error" -> messages("invalid.credentials"))
      }
    )
  }
}

package controllers

import javax.inject.Inject
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.impl.authenticators.SessionAuthenticator
import com.mohiva.play.silhouette.impl.providers._
import models.User
import models.services.UserService
import play.api.i18n.Messages
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.Action
import scala.concurrent.Future
import play.api.i18n.MessagesApi
import scala.reflect.ClassTag

/**
 * The social auth controller.
 *
 * @param userService The user service implementation.
 * @param AuthInfoRepository The auth info service implementation.
 * @param socialProviderRegistry The social provider registry.
 * @param env The Silhouette environment.
 */
class SocialAuthController @Inject() (
  userService: UserService,
  authInfoRepository: AuthInfoRepository,
  socialProviderRegistry: SocialProviderRegistry,
  messagesApi : MessagesApi,
  protected val env: Environment[User, SessionAuthenticator])
  extends Silhouette[User, SessionAuthenticator] with Logger {

  /**
   * Authenticates a user against a social provider.
   *
   * @param provider The ID of the provider to authenticate against.
   * @return The result to display.
   */
  def authenticate(provider: String) = Action.async { implicit request =>
    val messages = messagesApi.preferred(request)
    (socialProviderRegistry.get(provider) match {
      case Some(p: SocialProvider with CommonSocialProfileBuilder) =>
        import p.authInfoClassTag
        p.authenticate().flatMap {
          case Left(result) => Future.successful(result)
          case Right(authInfo) => for {
            _ <- Future.successful(println/*logger.debug*/(s"authInfo=${authInfo}"))
            profile <- p.retrieveProfile(authInfo)
            _ <- Future.successful(println/*logger.debug*/(s"profile=${profile}"))
            user <- userService.save(profile)
            _ <- Future.successful(println/*logger.debug*/(s"user=${user}"))
            _ <- Future.successful(println/*logger.debug*/(s"p.A classTag = ${implicitly[ClassTag[p.A]]}"))
            authInfoExists <- authInfoRepository.find[p.A](profile.loginInfo).map(_.isDefined)
            _ <- Future.successful(println/*logger.debug*/(s"authInfoExists=${authInfoExists}"))
            _ <- if(authInfoExists) {
                      println/*logger.debug*/("updating")
                      authInfoRepository.update[p.A](profile.loginInfo, authInfo) 
                 } else { 
                      println/*logger.debug*/("adding")
                      authInfoRepository.add[p.A](profile.loginInfo, authInfo)
                 }
            authenticator <- env.authenticatorService.create(profile.loginInfo)
            _ <- Future.successful(println/*logger.debug*/(s"authenticator=${authenticator}"))
            value <- env.authenticatorService.init(authenticator)
            _ <- Future.successful(println/*logger.debug*/(s"value=${value}"))
            result <- env.authenticatorService.embed(value, Redirect(routes.ApplicationController.index()))
            _ <- Future.successful(println/*logger.debug*/(s"result=${result}"))
          } yield {
            env.eventBus.publish(LoginEvent(user, request, messages))
            result
          }
        }
      case _ => Future.failed(new ProviderException(s"Cannot authenticate with unexpected social provider $provider"))
    }).recover {
      case e: ProviderException =>
        logger.error("Unexpected provider error", e)
        Redirect(routes.ApplicationController.signIn()).flashing("error" -> messages("could.not.authenticate"))
    }
  }
}

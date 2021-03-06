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
    logger.debug(s"authenticate: started with provider=${provider}")
    val messages = messagesApi.preferred(request)
    (socialProviderRegistry.get(provider) match {
      case Some(p: SocialProvider with CommonSocialProfileBuilder) =>
        logger.debug(s"authenticate: provider found=${p}")
        import p.authInfoClassTag
        p.authenticate().flatMap {
          case Left(result) => 
                logger.debug(s"authenticate: authenticated directly result=${result}")
                Future.successful(result)
          case Right(authInfo) => for {
            _ <- Future.successful(logger.debug(s"authenticate: must process authInfo=${authInfo}"))
            profile <- p.retrieveProfile(authInfo)
            _ <- Future.successful(logger.debug(s"authenticate: found profile=${profile}, loginInfo=${profile.loginInfo}"))
            user <- userService.save(profile)
            _ <- Future.successful(logger.debug(s"authenticate: found user=${user}"))
            authInfoExists <- authInfoRepository.find[p.A](profile.loginInfo).map(_.isDefined)
            _ <- if(authInfoExists) {
                      logger.debug(s"authenticate: authInfoExists=true updating")
                      authInfoRepository.update[p.A](profile.loginInfo, authInfo) 
                 } else { 
                      logger.debug(s"authenticate: authInfoExists=false adding")
                      authInfoRepository.add[p.A](profile.loginInfo, authInfo)
                 }
            authenticator <- env.authenticatorService.create(profile.loginInfo)
            _ <- Future.successful(logger.debug(s"authenticate: created authenticator=${authenticator}"))
            value <- env.authenticatorService.init(authenticator)
            _ <- Future.successful(logger.debug(s"authenticate: authenticator initialization returned value=${value}"))
            result <- env.authenticatorService.embed(value, Redirect(routes.ApplicationController.index()))
            _ <- Future.successful(logger.debug(s"authenticate: authenticator embedding returned result=${result}"))
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

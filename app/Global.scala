package app

import com.mohiva.play.silhouette.api.{ Logger, SecuredSettings }
import controllers.routes
import play.api.GlobalSettings
import play.api.i18n.{ Lang, Messages }
import play.api.mvc.Results._
import play.api.mvc.{ RequestHeader, Result }
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

/**
 * The global object.
 */
object Global extends Global

/**
 * The global configuration.
 */
trait Global extends GlobalSettings with SecuredSettings with Logger {
  
  /**
   * Called when a user is not authenticated.
   *
   * As defined by RFC 2616, the status code of the response should be 401 Unauthorized.
   *
   * @param request The request header.
   * @param lang The currently selected language.
   * @return The result to send to the client.
   */
  override def onNotAuthenticated(request: RequestHeader, messages: Messages)(implicit ec : ExecutionContext): Option[Future[Result]] = {
    Some(Future.successful(Redirect(routes.ApplicationController.signIn)))
  }

  /**
   * Called when a user is authenticated but not authorized.
   *
   * As defined by RFC 2616, the status code of the response should be 403 Forbidden.
   *
   * @param request The request header.
   * @param lang The currently selected language.
   * @return The result to send to the client.
   */
  override def onNotAuthorized(request: RequestHeader, messages : Messages)(implicit ec : ExecutionContext): Option[Future[Result]] = {
    Some(Future.successful(Redirect(routes.ApplicationController.signIn).flashing("error" -> messages("access.denied"))))
  }
}

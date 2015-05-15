package app

import com.mohiva.play.silhouette.api.{ SecuredSettings }
import controllers.routes
import play.api.GlobalSettings
import play.api.i18n.{ Lang, Messages }
import play.api.mvc.Results._
import play.api.mvc.{ RequestHeader, Result }
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import play.api.mvc.Filter
import play.api.Logger
import play.api.mvc.WithFilters

object AccessLoggingFilter extends Filter {
  
  val accessLogger = Logger("access")
  
  def apply(next: (RequestHeader) => Future[Result])(request: RequestHeader) : Future[Result] = {
    import play.api.libs.concurrent.Execution.Implicits.defaultContext
    val resultFuture = next(request)
    
    resultFuture.foreach(result => {
      val msg = s"method=${request.method} uri=${request.uri} remote-address=${request.remoteAddress}" +
        s" status=${result.header.status}";
      accessLogger.info(msg)
    })
    
    resultFuture
  }
}

/**
 * The global object.
 */
object Global extends Global

/**
 * The global configuration.
 */
class Global extends WithFilters(AccessLoggingFilter) with GlobalSettings with SecuredSettings {  
  
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

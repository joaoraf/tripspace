package app

import com.mohiva.play.silhouette.api.{ Logger, SecuredSettings }
import controllers.routes
import play.api.GlobalSettings
import play.api.i18n.{ Lang, Messages }
import play.api.mvc.Results._
import play.api.mvc.{ RequestHeader, Result }
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import play.api.mvc.Filter
import play.api.mvc.WithFilters
import play.api.Logger


object AccessLoggingFilter extends Filter {
  
  val accessLogger = Logger("access")
  
  def apply(next: (RequestHeader) => Future[Result])(request: RequestHeader) : Future[Result] = {
    import play.api.libs.concurrent.Execution.Implicits.defaultContext
    if(request.uri.startsWith("/assets") || request.uri.startsWith("/webjars/")) {
      next(request)
    } else {
      for {
        _ <- Future.successful(accessLogger.info(s"start: method=${request.method} uri=${request.uri} remote-address=${request.remoteAddress}"))
        resultFuture = next(request)
        _ <- Future.successful(resultFuture.onFailure { 
          case ex : Exception =>
             accessLogger.error(s"end: method=${request.method} uri=${request.uri} remote-address=${request.remoteAddress}" +
                s" exception=${ex}",ex)
        }) 
        result <- resultFuture      
        _ <- Future.successful(accessLogger.info(s"end: method=${request.method} uri=${request.uri} remote-address=${request.remoteAddress}" +
          s" status=${result.header.status}"))
      } yield { result }
    }
  }
}

/**
 * The global object.
 */
object Global extends GlobalImpl

/**
 * The global configuration.
 */
class GlobalImpl extends WithFilters(AccessLoggingFilter) with SecuredSettings {  
  
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

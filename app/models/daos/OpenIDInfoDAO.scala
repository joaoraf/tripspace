package models.daos

import com.mohiva.play.silhouette.api.LoginInfo

import com.mohiva.play.silhouette.impl.daos.DelegableAuthInfoDAO
import com.mohiva.play.silhouette.impl.providers.OpenIDInfo
import models.daos.OpenIDInfoDAO._
import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

/**
 * The DAO to store the OpenID information.
 *
 * Note: Not thread safe, demo only.
 */
class OpenIDInfoDAO extends DelegableAuthInfoDAO[OpenIDInfo] {

  /**
   * Saves the OpenID info.
   *
   * @param loginInfo The login info for which the auth info should be saved.
   * @param authInfo The OpenID info to save.
   * @return The saved OpenID info.
   */
  def save(loginInfo: LoginInfo, authInfo: OpenIDInfo): Future[OpenIDInfo] = {
    data += (loginInfo -> authInfo)
    Future.successful(authInfo)
  }

  /**
   * Finds the OpenID info which is linked with the specified login info.
   *
   * @param loginInfo The linked login info.
   * @return The retrieved OpenID info or None if no OpenID info could be retrieved for the given login info.
   */
  def find(loginInfo: LoginInfo)(implicit ec : ExecutionContext): Future[Option[OpenIDInfo]] = {
    Future.successful(data.get(loginInfo))
  }
  
  /**
   * Adds new auth info for the given login info.
   *
   * @param loginInfo The login info for which the auth info should be added.
   * @param authInfo The auth info to add.
   * @return The added auth info.
   */
  def add(loginInfo: LoginInfo, authInfo: OpenIDInfo)(implicit ec: ExecutionContext): Future[OpenIDInfo] = {
    Future.successful(data += (loginInfo -> authInfo)).map(_ => authInfo)
  }

  /**
   * Updates the auth info for the given login info.
   *
   * @param loginInfo The login info for which the auth info should be updated.
   * @param authInfo The auth info to update.
   * @return The updated auth info.
   */
  def update(loginInfo: LoginInfo, authInfo: OpenIDInfo)(implicit ec: ExecutionContext): Future[OpenIDInfo] = {
    Future.successful(data += (loginInfo -> authInfo)).map(_ => authInfo)    
  }

  /**
   * Removes the auth info for the given login info.
   *
   * @param loginInfo The login info for which the auth info should be removed.
   * @return A future to wait for the process to be completed.
   */
  def remove(loginInfo: LoginInfo)(implicit ec: ExecutionContext): Future[Unit] = {
    Future.successful(data -= loginInfo)
  }
}

/**
 * The companion object.
 */
object OpenIDInfoDAO {

  /**
   * The data store for the OpenID info.
   */
  var data: mutable.HashMap[LoginInfo, OpenIDInfo] = mutable.HashMap()
}

package models.daos.slickdaos

import com.mohiva.play.silhouette.api.LoginInfo

import com.mohiva.play.silhouette.impl.daos.DelegableAuthInfoDAO
import com.mohiva.play.silhouette.impl.providers.OAuth1Info
import scala.concurrent.Future
import DBTableDefinitions._
import slick.driver.PostgresDriver.api._
import play.api.db.slick._
import slick.profile.RelationalProfile
import javax.inject.Inject
import scala.concurrent.ExecutionContext


/**
 * The DAO to store the OAuth1 information.
 */
class OAuth1InfoDAOSlick @Inject()(dbConfigProvider : DatabaseConfigProvider) extends DelegableAuthInfoDAO[OAuth1Info] {
  val dbConfig = dbConfigProvider.get[RelationalProfile]
  import dbConfig.driver.api._

  import dbConfig.db
  
  
  /**
   * Finds the OAuth1 info which is linked with the specified login info.
   *
   * @param loginInfo The linked login info.
   * @return The retrieved OAuth1 info or None if no OAuth1 info could be retrieved for the given login info.
   */
  def find(loginInfo: LoginInfo)(implicit ec : ExecutionContext): Future[Option[OAuth1Info]] = 
    db.run (findAction(loginInfo))
  
  def findAction(loginInfo: LoginInfo)(implicit ec : ExecutionContext) = (for {
    data <- (for {
      sli <- slickLoginInfos
      if sli.providerID === loginInfo.providerID &&
         sli.providerKey === loginInfo.providerKey
      oau1 <- slickOAuth1Infos
      if sli.id === oau1.loginInfoId    
    } yield (oau1.token, oau1.secret) ).result.headOption    
  } yield (data.map { case (token,secret) => OAuth1Info(token, secret) })).transactionally
  
  /**
   * Adds new auth info for the given login info.
   *
   * @param loginInfo The login info for which the auth info should be added.
   * @param authInfo The auth info to add.
   * @return The added auth info.
   */
  def add(loginInfo: LoginInfo, authInfo: OAuth1Info)(implicit ec: ExecutionContext): Future[OAuth1Info] =
      db.run(addAction(loginInfo,authInfo))

  def addAction(loginInfo: LoginInfo, authInfo: OAuth1Info)(implicit ec: ExecutionContext): DBIO[OAuth1Info] =
    (for {      
       optLoginInfoId <- slickLoginInfos
             .filter(x => x.providerKey === loginInfo.providerKey &&
                          x.providerID === loginInfo.providerID)
             .map(_.id).result.headOption
       _ <- optLoginInfoId match {
         case None => throw new RuntimeException(s"LoginInfo not found for ${loginInfo.providerID}:${loginInfo.providerKey}")
         case Some(loginInfoId) => 
           slickOAuth1Infos += DBOAuth1Info(None, authInfo.token, authInfo.secret, loginInfoId) 
       }
     } yield (authInfo)).transactionally
    
  /**
   * Updates the auth info for the given login info.
   *
   * @param loginInfo The login info for which the auth info should be updated.
   * @param authInfo The auth info to update.
   * @return The updated auth info.
   */
  def update(loginInfo: LoginInfo, authInfo: OAuth1Info)(implicit ec: ExecutionContext): Future[OAuth1Info] =
    db.run(updateAction(loginInfo,authInfo))  
    
  def updateAction(loginInfo: LoginInfo, authInfo: OAuth1Info)(implicit ec: ExecutionContext): DBIO[OAuth1Info] =
    (for {                      
       optLoginInfoId <- slickLoginInfos
             .filter(x => x.providerKey === loginInfo.providerKey &&
                          x.providerID === loginInfo.providerID)
             .map(_.id).result.headOption
       _ <- optLoginInfoId match {
         case None => throw new RuntimeException(s"LoginInfo not found for ${loginInfo.providerID}:${loginInfo.providerKey}")
         case Some(loginInfoId) => for {
           optAuthId <- slickLoginInfos
             .join(slickOAuth1Infos)
             .on({case (sli,soai1) => sli.id === soai1.loginInfoId})
             .map(_._2.id).result.headOption
           _ <- optAuthId match {
               case None => throw new RuntimeException(s"no oauth1 info found on db for ${loginInfo.providerID}:${loginInfo.providerKey}")
               case Some(authId) => 
                   slickOAuth1Infos
                       .filter(_.id === authId)
                       .map(x => (x.token,x.secret))
                       .update((authInfo.token,authInfo.secret))
           }
         } yield (())
       } 
    } yield { authInfo }).transactionally

  /**
   * Removes the auth info for the given login info.
   *
   * @param loginInfo The login info for which the auth info should be removed.
   * @return A future to wait for the process to be completed.
   */
  def remove(loginInfo: LoginInfo)(implicit ec: ExecutionContext): Future[Unit] =
        db.run(removeAction(loginInfo))
        
  def removeAction(loginInfo: LoginInfo)(implicit ec: ExecutionContext): DBIO[Unit] = (for {
      _ <- slickOAuth1Infos.filter(
          _.loginInfoId in (
                slickLoginInfos
                  .filter(x => 
                     x.providerID === loginInfo.providerID &&
                     x.providerKey === loginInfo.providerKey)
                  .map(_.id))
        ).delete      
  } yield (())).transactionally
}

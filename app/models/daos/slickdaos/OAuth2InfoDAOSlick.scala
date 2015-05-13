package models.daos.slickdaos

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.daos.DelegableAuthInfoDAO
import com.mohiva.play.silhouette.impl.providers.OAuth2Info
import play.api.db.slick._
import scala.concurrent.Future
import DBTableDefinitions._
import slick.profile.RelationalProfile
import com.mohiva.play.silhouette.impl.providers.OAuth2Info
import javax.inject.Inject
import scala.concurrent.ExecutionContext
import DBTableDefinitions._
import slick.driver.PostgresDriver.api._


/**
 * The DAO to store the OAuth2 information.
 */
class OAuth2InfoDAOSlick @Inject()(dbConfigProvider : DatabaseConfigProvider) extends DelegableAuthInfoDAO[OAuth2Info] {
  val dbConfig = dbConfigProvider.get[RelationalProfile]
  import dbConfig.driver.api._

  import dbConfig.db

  

  /**
   * Finds the OAuth2 info which is linked with the specified login info.
   *
   * @param loginInfo The linked login info.
   * @return The retrieved OAuth2 info or None if no OAuth2 info could be retrieved for the given login info.
   */
  def find(loginInfo: LoginInfo)(implicit ec : ExecutionContext): Future[Option[OAuth2Info]] = 
    db.run (findAction(loginInfo))
  
  def findAction(loginInfo: LoginInfo)(implicit ec : ExecutionContext) = (for {
    data <- (for {
      sli <- slickLoginInfos
      if sli.providerID === loginInfo.providerID &&
         sli.providerKey === loginInfo.providerKey
      oau2 <- slickOAuth2Infos
      if sli.id === oau2.loginInfoId    
    } yield (oau2) ).result.headOption    
  } yield (data.map { case oau2 => 
      OAuth2Info(
          oau2.accessToken, 
          oau2.tokenType, 
          oau2.expiresIn, 
          oau2.refreshToken) })).transactionally
          
   /**
   * Adds new auth info for the given login info.
   *
   * @param loginInfo The login info for which the auth info should be added.
   * @param authInfo The auth info to add.
   * @return The added auth info.
   */
  def add(loginInfo: LoginInfo, authInfo: OAuth2Info)(implicit ec: ExecutionContext): Future[OAuth2Info] =
      db.run(addAction(loginInfo,authInfo))

  def addAction(loginInfo: LoginInfo, authInfo: OAuth2Info)(implicit ec: ExecutionContext): DBIO[OAuth2Info] =
    (for {      
       optLoginInfoId <- slickLoginInfos
             .filter(x => x.providerKey === loginInfo.providerKey &&
                          x.providerID === loginInfo.providerID)
             .map(_.id).result.headOption
       _ <- optLoginInfoId match {
         case None => throw new RuntimeException(s"LoginInfo not found for ${loginInfo.providerID}:${loginInfo.providerKey}")
         case Some(loginInfoId) => 
           slickOAuth2Infos += DBOAuth2Info(None, authInfo.accessToken, authInfo.tokenType, authInfo.expiresIn, authInfo.refreshToken, loginInfoId) 
       }
     } yield (authInfo)).transactionally
    
  /**
   * Updates the auth info for the given login info.
   *
   * @param loginInfo The login info for which the auth info should be updated.
   * @param authInfo The auth info to update.
   * @return The updated auth info.
   */
  def update(loginInfo: LoginInfo, authInfo: OAuth2Info)(implicit ec: ExecutionContext): Future[OAuth2Info] =
    db.run(updateAction(loginInfo,authInfo))  
    
  def updateAction(loginInfo: LoginInfo, authInfo: OAuth2Info)(implicit ec: ExecutionContext): DBIO[OAuth2Info] =
    (for {                      
       optLoginInfoId <- slickLoginInfos
             .filter(x => x.providerKey === loginInfo.providerKey &&
                          x.providerID === loginInfo.providerID)
             .map(_.id).result.headOption
       _ <- optLoginInfoId match {
         case None => throw new RuntimeException(s"LoginInfo not found for ${loginInfo.providerID}:${loginInfo.providerKey}")
         case Some(loginInfoId) => for {
           optAuthId <- slickLoginInfos
             .join(slickOAuth2Infos)
             .on({case (sli,soai1) => sli.id === soai1.loginInfoId})
             .map(_._2.id).result.headOption
           _ <- optAuthId match {
               case None => throw new RuntimeException(s"no oauth2 info found on db for ${loginInfo.providerID}:${loginInfo.providerKey}")
               case Some(authId) => 
                   slickOAuth2Infos
                       .filter(_.id === authId)
                       .map(x => (x.accessToken,x.tokenType, x.expiresIn, x.refreshToken))
                         .update((authInfo.accessToken, authInfo.tokenType, authInfo.expiresIn, authInfo.refreshToken))
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
      _ <- slickOAuth2Infos.filter(
          _.loginInfoId in (
                slickLoginInfos
                  .filter(x => 
                     x.providerID === loginInfo.providerID &&
                     x.providerKey === loginInfo.providerKey)
                  .map(_.id))
        ).delete      
  } yield (())).transactionally

}

package models.daos.slickdaos

import com.mohiva.play.silhouette.api.LoginInfo

import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.impl.daos.DelegableAuthInfoDAO
import play.api.db.slick._
import scala.concurrent.Future
import DBTableDefinitions._
import slick.profile.RelationalProfile
import javax.inject.Inject
import scala.concurrent.ExecutionContext
import DBTableDefinitions._
import slick.driver.PostgresDriver.api._




/**
 * The DAO to store the password information.
 */
class PasswordInfoDAOSlick @Inject()(dbConfigProvider : DatabaseConfigProvider) extends DelegableAuthInfoDAO[PasswordInfo] {
  val dbConfig = dbConfigProvider.get[RelationalProfile]
  import dbConfig.driver.api._

  import dbConfig.db  

  /**
   * Saves the password info.
   *
   * @param loginInfo The login info for which the auth info should be saved.
   * @param authInfo The password info to save.
   * @return The saved password info or None if the password info couldn't be saved.
   */
  def save(loginInfo: LoginInfo, authInfo: PasswordInfo)(implicit ec : ExecutionContext): Future[PasswordInfo] = 
      db.run(saveAction(loginInfo,authInfo))
  
  def loginId(loginInfo : LoginInfo) : DBIO[Option[Long]] =
    slickLoginInfos
                 .filter(x => x.providerKey === loginInfo.providerKey &&
                              x.providerID === loginInfo.providerID)
                 .map(_.id).result.headOption
      
  def saveAction(loginInfo: LoginInfo, authInfo: PasswordInfo)
        (implicit ec : ExecutionContext): DBIO[PasswordInfo] = 
    (for {       
      optLoginInfoId <- loginId(loginInfo)     
      _ <- optLoginInfoId match {
              case None => throw new RuntimeException(s"Unexpected state: loginInfo not in db: ${loginInfo.providerID}:${loginInfo.providerKey}")
              case Some(loginInfoId) => for {
                _ <- slickPasswordInfos.filter(_.loginInfoId === loginInfoId).delete
                _ <- slickPasswordInfos += DBPasswordInfo(authInfo.hasher, authInfo.password, authInfo.salt, loginInfoId)
              } yield (())
            }          
    } yield(authInfo)).transactionally
  
  /**
   * Finds the password info which is linked with the specified login info.
   *
   * @param loginInfo The linked login info.
   * @return The retrieved password info or None if no password info could be retrieved for the given login info.
   */
  def find(loginInfo: LoginInfo)(implicit ec : ExecutionContext): Future[Option[PasswordInfo]] = 
    db.run(findAction(loginInfo))
    
  def findAction(loginInfo: LoginInfo)(implicit ec : ExecutionContext): DBIO[Option[PasswordInfo]] = (
    slickLoginInfos
       .filter(x => x.providerKey === loginInfo.providerKey &&
               x.providerID === loginInfo.providerID)
       .join(slickPasswordInfos)
       .on({case (sli,spi) => sli.id === spi.loginInfoId })
       .map(_._2)
       .result.headOption.map(_.map { passwordInfo =>
         PasswordInfo(passwordInfo.hasher, passwordInfo.password, passwordInfo.salt)
       })
  ).transactionally  
  
  /**
   * Adds new auth info for the given login info.
   *
   * @param loginInfo The login info for which the auth info should be added.
   * @param authInfo The auth info to add.
   * @return The added auth info.
   */
  def add(loginInfo: LoginInfo, authInfo: PasswordInfo)(implicit ec: ExecutionContext): Future[PasswordInfo] =
      db.run(addAction(loginInfo,authInfo))

  def addAction(loginInfo: LoginInfo, authInfo: PasswordInfo)(implicit ec: ExecutionContext): DBIO[PasswordInfo] =
    (for {      
       optLoginInfoId <- slickLoginInfos
             .filter(x => x.providerKey === loginInfo.providerKey &&
                          x.providerID === loginInfo.providerID)
             .map(_.id).result.headOption
       _ <- optLoginInfoId match {
         case None => throw new RuntimeException(s"LoginInfo not found for ${loginInfo.providerID}:${loginInfo.providerKey}")
         case Some(loginInfoId) => for {
           _ <- slickPasswordInfos.filter(_.loginInfoId === loginInfoId).delete
           _ <- slickPasswordInfos += DBPasswordInfo(authInfo.hasher, authInfo.password, authInfo.salt, loginInfoId) 
         } yield (())
       }
     } yield (authInfo)).transactionally
    
  /**
   * Updates the auth info for the given login info.
   *
   * @param loginInfo The login info for which the auth info should be updated.
   * @param authInfo The auth info to update.
   * @return The updated auth info.
   */
  def update(loginInfo: LoginInfo, authInfo: PasswordInfo)(implicit ec: ExecutionContext): Future[PasswordInfo] =
    db.run(updateAction(loginInfo,authInfo))  
    
  def updateAction(loginInfo: LoginInfo, authInfo: PasswordInfo)(implicit ec: ExecutionContext): DBIO[PasswordInfo] =
    (for {                      
       optLoginInfoId <- slickLoginInfos
             .filter(x => x.providerKey === loginInfo.providerKey &&
                          x.providerID === loginInfo.providerID)
             .map(_.id).result.headOption
       _ <- optLoginInfoId match {
         case None => throw new RuntimeException(s"LoginInfo not found for ${loginInfo.providerID}:${loginInfo.providerKey}")
         case Some(loginInfoId) => 
           slickPasswordInfos += DBPasswordInfo(authInfo.hasher, authInfo.password, authInfo.salt, loginInfoId)          
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
      _ <- slickPasswordInfos.filter(
          _.loginInfoId in (
                slickLoginInfos
                  .filter(x => 
                     x.providerID === loginInfo.providerID &&
                     x.providerKey === loginInfo.providerKey)
                  .map(_.id))
        ).delete      
  } yield (())).transactionally
}

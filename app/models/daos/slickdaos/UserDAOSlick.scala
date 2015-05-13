package models.daos.slickdaos

import models.User
import play.api.db.slick._
import slick.driver.PostgresDriver.simple._
import DBTableDefinitions._
import com.mohiva.play.silhouette.api.LoginInfo
import scala.concurrent.Future
import java.util.UUID
import play.Logger
import models.daos.UserDAO
import play.api.db.DB
import javax.inject.Inject
import slick.profile.RelationalProfile
import scala.concurrent.ExecutionContext

/**
 * Give access to the user object using Slick
 */
class UserDAOSlick @Inject() (dbConfigProvider : DatabaseConfigProvider, slickQueries : SlickQueries) extends UserDAO {
  lazy val dbConfig = dbConfigProvider.get[RelationalProfile]
  
  import dbConfig.driver.api._
  val db = dbConfig.db
  /**
   * Finds a user by its login info.
   *
   * @param loginInfo The login info of the user to find.
   * @return The found user or None if no user for the given login info could be found.
   */
  def find(loginInfo: LoginInfo)(implicit ec : ExecutionContext) = 
    db.run (slickQueries.user.find(loginInfo))
    

  /**
   * Finds a user by its user ID.
   *
   * @param userID The ID of the user to find.
   * @return The found user or None if no user for the given ID could be found.
   */
  def find(userID: UUID)(implicit ec : ExecutionContext) = db.run (slickQueries.user.find(userID))

  /**
   * Saves a user.
   *
   * @param user The user to save.
   * @return The saved user.
   */
  def save(user: User)(implicit ec : ExecutionContext) = db.run (slickQueries.user.save(user))
}

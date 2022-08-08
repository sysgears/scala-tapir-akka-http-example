package com.example.dao

import com.example.models.Roles.RoleType
import com.example.models.{Roles, User}
import io.getquill
import io.getquill.NamingStrategy
import io.getquill.context.jdbc.JdbcContext
import io.getquill.context.sql.idiom.SqlIdiom

import scala.concurrent.{ExecutionContext, Future}

/**
 * Dao for user.
 *
 * @param context important stuff. Uses for connection to database.
 * @param ec for async work.
 */
class UserDao(context: JdbcContext[_ <: SqlIdiom, _ <: NamingStrategy])(implicit ec: ExecutionContext) {

  import context._

  /** Enum values mapping for the database. */
  implicit val encodeRole = getquill.MappedEncoding[RoleType, Int](_.id)
  implicit val decodeRole = getquill.MappedEncoding[Int, RoleType](roleId => Roles.withId(roleId))

  /** Query schema. Closest analogue - table in Slick. */
  private val users = quote {
    querySchema[User]("users")
  }

  /** Creates user and returns generated id. */
  def createUser(user: User): Future[Long] = Future {
    run(users.insertValue(lift(user)).returningGenerated(_.id))
  }

  /**
   * Updates user.
   *
   * @param user user to update
   * @return update result.
   */
  def updateUser(user: User): Future[Long] = Future {
    run(users.filter(_.id == lift(user.id)).updateValue(lift(user)))
  }

  /** Removes user. */
  def deleteUser(userId: Long): Future[Long] = Future {
    run(users.filter(_.id == lift(userId)).delete)
  }

  /** Searches user by id. */
  def find(userId: Long): Future[Option[User]] = Future {
    run(users.filter(_.id == lift(userId))).headOption
  }

  /** Searches user by email. */
  def findByEmail(email: String): Future[Option[User]] = Future {
    run(users.filter(_.email == lift(email))).headOption
  }

  /** Searches users by ids. */
  def findByIds(userIds: Seq[Long]): Future[List[User]] = Future {
    run(users.filter(user => liftQuery(userIds).contains(user.id)))
  }
}

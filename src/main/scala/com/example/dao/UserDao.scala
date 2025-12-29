package com.example.dao

import com.example.dao
import com.example.models.Roles.RoleType
import com.example.models.{Roles, User}
import io.getquill
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import zio.{ZIO, ZLayer}

import java.sql.SQLException

/**
 * Dao for user.
 */
object UserDao {

  type UserRepository = UserDao.Service

  trait Service {
    def createUser(user: User): ZIO[Any, SQLException, Long]
    def updateUser(user: User): ZIO[Any, SQLException, Long]
    def deleteUser(userId: String): ZIO[Any, SQLException, Long]
    def find(userId: String): ZIO[Any, SQLException, Option[User]]
    def findByEmail(email: String): ZIO[Any, SQLException, Option[User]]
    def findByIds(userIds: Seq[String]): ZIO[Any, SQLException, List[User]]
  }

  val live: ZLayer[Quill.Postgres[SnakeCase], Nothing, dao.UserDao.UserRepository] = ZLayer {
    for {
      context <- ZIO.service[Quill.Postgres[SnakeCase]]
    } yield {
      new Service {
        import context._

        /** Enum values mapping for the database. */
        implicit val encodeRole = getquill.MappedEncoding[RoleType, Int](_.id)
        implicit val decodeRole = getquill.MappedEncoding[Int, RoleType](roleId => Roles.withId(roleId))

        /** Query schema. Closest analogue - table in Slick. */
        private val users = quote {
          querySchema[User]("users")
        }

        override def createUser(user: User): ZIO[Any, SQLException, Long] =
          run(users.insertValue(lift(user)))

        override def updateUser(user: User): ZIO[Any, SQLException, Long] =
          run(users.filter(_.id == lift(user.id)).updateValue(lift(user)))

        override def deleteUser(userId: String): ZIO[Any, SQLException, Long] =
          run(users.filter(_.id == lift(userId)).delete)

        override def find(userId: String): ZIO[Any, SQLException, Option[User]] =
          run(users.filter(_.id == lift(userId))).map(_.headOption)

        override def findByEmail(email: String): ZIO[Any, SQLException, Option[User]] =
          run(users.filter(_.email == lift(email))).map(_.headOption)

        override def findByIds(userIds: Seq[String]): ZIO[Any, SQLException, List[User]] =
          run(users.filter(user => liftQuery(userIds).contains(user.id)))
      }
    }
  }
}

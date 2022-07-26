package com.example.dao

import com.example.models.User
import io.getquill.NamingStrategy
import io.getquill.context.jdbc.JdbcContext
import io.getquill.context.sql.idiom.SqlIdiom

import scala.concurrent.{ExecutionContext, Future}

class UserDao(context: JdbcContext[_ <: SqlIdiom, _ <: NamingStrategy])(implicit ec: ExecutionContext) {

  import context._

  private val users = quote {
    querySchema[User]("users")
  }

  def createUser(user: User): Future[Long] = Future {
    run(users.insertValue(lift(user)).returningGenerated(_.id))
  }

  def updateUser(user: User): Future[Long] = Future {
    run(users.filter(_.id == lift(user.id)).updateValue(lift(user)))
  }

  def deleteUser(userId: Long): Future[Long] = Future {
    run(users.filter(_.id == lift(userId)).delete)
  }

  def find(userId: Long): Future[Option[User]] = Future {
    run(users.filter(_.id == lift(userId))).headOption
  }

  def findByEmail(email: String): Future[Option[User]] = Future {
    run(users.filter(_.email == lift(email))).headOption
  }
}

package com.example.auth

import com.example.auth.Jwt.JwtService
import com.example.dao.UserDao.UserRepository
import com.example.errors.{ErrorInfo, InternalServerError, Unauthorized}
import com.example.models.User
import com.typesafe.scalalogging.LazyLogging
import zio.{ZIO, ZLayer}


/**
 * Contains authentication functionality.
 *
 * @param jwtService service, which works with jwt tokens.
 * @param ec for async futures.
 */
object TapirAuthentication extends LazyLogging {

  type TapirAuth = TapirAuthentication.Service

  trait Service {
    /** Extracts user from token. Return either Status code with error message or user. */
    def authenticate(token: String): ZIO[Any, ErrorInfo, User]
  }

  def authenticate(token: String): ZIO[TapirAuth, ErrorInfo, User] = ZIO.serviceWithZIO[TapirAuth](_.authenticate(token))

  val live = ZLayer {
    for {
      jwtService <- ZIO.service[JwtService]
      userDao <- ZIO.service[UserRepository]
    } yield {
      new Service {

        override def authenticate(token: String): ZIO[Any, ErrorInfo, User] =
          jwtService.extractUserIdFromJwt(token).mapError(error => Unauthorized("Token is expired. You need to log in first")).flatMap {
            case Some(userId) =>
              userDao.find(userId).mapError { error =>
                logger.error("Intercepted error within authenticate action", error)
                InternalServerError("Internal error")
              }.flatMap {
                case Some(user) => ZIO.succeed(user)
                case None => ZIO.fail(Unauthorized("user from token is not found"))
              }
            case None => ZIO.fail(Unauthorized("user from token is not found"))
          }
      }
    }
  }
}

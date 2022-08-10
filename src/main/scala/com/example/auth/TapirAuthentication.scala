package com.example.auth

import com.example.errors.{ErrorInfo, Unauthorized}
import com.example.models.User

import scala.concurrent.{ExecutionContext, Future}

/**
 * Contains authentication functionality.
 *
 * @param jwtService service, which works with jwt tokens.
 * @param ec for async futures.
 */
class TapirAuthentication(jwtService: JwtService)(implicit ec: ExecutionContext) {

  /** Extracts user from token. Return either Status code with error message or user. */
  def authenticate(token: String): Future[Either[ErrorInfo, User]] = {
    jwtService.extractUserFromJwt(token).map {
      case Left(exception) => Left(Unauthorized("Token is expired. You need to log in first"))
      case Right(userOpt) => userOpt match {
        case Some(user) => Right(user)
        case None => Left(Unauthorized("user from token is not found"))
      }

    }
  }
}

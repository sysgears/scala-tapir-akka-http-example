package com.example.auth

import com.example.models.{ErrorMessage, User}
import sttp.model.StatusCode

import scala.concurrent.{ExecutionContext, Future}

/**
 * Contains authentication functionality.
 *
 * @param jwtService service, which works with jwt tokens.
 * @param ec for async futures.
 */
class TapirAuthentication(jwtService: JwtService)(implicit ec: ExecutionContext) {

  /** Extracts user from token. Return either Status code with error message or user. */
  def authenticate(token: String): Future[Either[(StatusCode, ErrorMessage), User]] = {
    jwtService.extractUserFromJwt(token).map {
      case Left(exception) => Left((StatusCode.Unauthorized, ErrorMessage("Token is expired. You need to log in first")))
      case Right(userOpt) => userOpt match {
        case Some(user) => Right(user)
        case None => Left((StatusCode.Unauthorized, ErrorMessage("user from token is not found")))
      }

    }
  }
}

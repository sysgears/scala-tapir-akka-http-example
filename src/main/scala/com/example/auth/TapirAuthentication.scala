package com.example.auth

import com.example.dao.UserDao
import com.example.models.{AuthError, User}
import sttp.model.StatusCode

import scala.concurrent.{ExecutionContext, Future}

class TapirAuthentication(jwtService: JwtService, userDao: UserDao)(implicit ec: ExecutionContext) {

  def authenticate(token: String): Future[Either[(StatusCode, AuthError), User]] = {
    jwtService.extractUserFromJwt(token).map {
      case Some(user) => Right(user)
      case None => Left((StatusCode.Unauthorized, AuthError("user from token is not found")))
    }
  }
}

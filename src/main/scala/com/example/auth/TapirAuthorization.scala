package com.example.auth

import com.example.models.{AuthError, User}
import sttp.model.StatusCode

import scala.concurrent.Future

object TapirAuthorization {

  def isAuthorized(user: User, roles: List[String]): Future[Either[(StatusCode, AuthError), User]] =
    Future.successful(if (roles.contains(user.role)) Right(user) else Left((StatusCode.Forbidden, AuthError("user is not allowed to use this endpoint"))))
}

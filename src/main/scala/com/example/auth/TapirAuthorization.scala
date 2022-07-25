package com.example.auth

import com.example.models.User

import scala.concurrent.Future

object TapirAuthorization {

  def isAuthorized(user: User, roles: List[String]): Future[Either[AuthError, User]] =
    Future.successful(if (roles.contains(user.role)) Right(user) else Left(AuthError(403)))
}

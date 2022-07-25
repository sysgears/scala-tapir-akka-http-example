package com.example.auth

import java.time.{LocalDateTime, ZoneId}

import com.example.models.{AuthError, User}
import sttp.model.StatusCode

import scala.concurrent.{ExecutionContext, Future}

class TapirAuthentication()(implicit ec: ExecutionContext) {

  val placeholderUser = User(0, "test", "+7777777777", "test@example.com", "", "49050", "Dnipro", "test street, 46", "User", LocalDateTime.now(ZoneId.of("UTC")))

  def authenticate(token: String): Future[Either[(StatusCode, AuthError), User]] = if (token.startsWith("test")) {
    Future.successful(Right(placeholderUser))
  } else Future.successful(Left((StatusCode.Unauthorized, AuthError("token is not valid"))))
}

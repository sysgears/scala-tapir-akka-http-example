package com.example.auth

import com.example.auth.TapirAuthorization.isAuthorized
import com.example.models.{AuthError, User}
import com.example.utils.Util.foldEitherOfFuture
import sttp.model.StatusCode
import sttp.tapir.generic.auto._
import sttp.tapir.{auth, endpoint}
import sttp.tapir._
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.PartialServerEndpoint

import scala.concurrent.{ExecutionContext, Future}

class TapirSecurity(authentication: TapirAuthentication)(implicit ec: ExecutionContext) {
  def tapirSecurityEndpoint(roles: List[String]): PartialServerEndpoint[String, User, Unit, (StatusCode, AuthError), Unit, Any, Future] = endpoint.securityIn(auth.bearer[String]())
    .errorOut(statusCode).errorOut(jsonBody[AuthError])
    .serverSecurityLogic(authentication.authenticate(_).flatMap {
      either => foldEitherOfFuture(either.map(isAuthorized(_, roles))).map {
        case Left(value) => Left(value)
        case Right(value) => value match {
          case Left(value) => Left(value)
          case Right(value) => Right(value)
        }
      }
    })
}

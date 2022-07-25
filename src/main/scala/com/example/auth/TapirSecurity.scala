package com.example.auth

import com.example.auth.TapirAuthorization.isAuthorized
import com.example.models.User
import com.example.utils.Util.foldEitherOfFuture
import sttp.tapir.{auth, endpoint}
import sttp.tapir._
import sttp.tapir.server.PartialServerEndpoint

import scala.concurrent.{ExecutionContext, Future}

class TapirSecurity(authentication: TapirAuthentication)(implicit ec: ExecutionContext) {
  def tapirSecurityEndpoint(roles: List[String]): PartialServerEndpoint[String, User, Unit, Unit, Unit, Any, Future] = endpoint.securityIn(auth.bearer[String]())
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

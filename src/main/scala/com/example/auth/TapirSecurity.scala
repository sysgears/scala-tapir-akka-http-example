package com.example.auth

import com.example.models.Roles.RoleType
import com.example.models.{ErrorMessage, User}
import com.example.utils.Util.foldEitherOfFuture
import sttp.model.StatusCode
import sttp.tapir.generic.auto._
import sttp.tapir.{auth, endpoint}
import sttp.tapir._
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.PartialServerEndpoint

import scala.concurrent.{ExecutionContext, Future}

class TapirSecurity(authentication: TapirAuthentication)(implicit ec: ExecutionContext) {

  def tapirSecurityEndpoint(roles: List[RoleType]): PartialServerEndpoint[String, User, Unit, (StatusCode, ErrorMessage), Unit, Any, Future] = endpoint.securityIn(auth.bearer[String]())
    .errorOut(statusCode).errorOut(jsonBody[ErrorMessage])
    .serverSecurityLogic(authentication.authenticate(_).flatMap {
      either => foldEitherOfFuture(either.map(isAuthorized(_, roles))).map(_.flatten)
    })

  def isAuthorized(user: User, roles: List[RoleType]): Future[Either[(StatusCode, ErrorMessage), User]] =
    Future.successful(if (roles.isEmpty || roles.contains(user.role)) Right(user) else Left((StatusCode.Forbidden, ErrorMessage("user is not allowed to use this endpoint"))))
}

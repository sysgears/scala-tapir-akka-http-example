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

/**
 * Configures security endpoint.
 *
 * @param authentication authentication service.
 * @param ec for futures.
 */
class TapirSecurity(authentication: TapirAuthentication)(implicit ec: ExecutionContext) {

  /**
   * Creates secured endpoint with role restriction from argument. If role list is empty - authorization is disabled
   *
   * PartialServerEndpoint explained: [Security input, Security output, Input, Error response, Output, idk what is this, wrapper (in most cases - future)]
   * In security endpoint defined Security input - bearer token, security output - user,
   *    error response - tuple of status code with error message object and wrapper.
   */
  def tapirSecurityEndpoint(roles: List[RoleType]): PartialServerEndpoint[String, User, Unit, (StatusCode, ErrorMessage), Unit, Any, Future] =
    endpoint // base tapir endpoint
      .securityIn(auth.bearer[String]().description("Bearer token from Authorization header")) // defining security input
      .errorOut(statusCode.description("Custom status code for different error situations")) // defining custom status code for different error situations
      .errorOut(jsonBody[ErrorMessage].description("Send error message").example(ErrorMessage("test error message."))) // defining error response (you can chain it and you need to provide in tuple)
      .serverSecurityLogic(authentication.authenticate(_).flatMap {
        // define security logic here. For example, here is authentication, chained with authorization
        either => foldEitherOfFuture(either.map(isAuthorized(_, roles))).map(_.flatten)
      })

  /**
   * Authorization filter function - checks user for present roles.
   * @param user user to check
   * @param roles restricted roles to check. If empty - skips authorization.
   * @return either error with Forbidden status code or user.
   */
  def isAuthorized(user: User, roles: List[RoleType]): Future[Either[(StatusCode, ErrorMessage), User]] =
    Future.successful(if (roles.isEmpty || roles.contains(user.role)) Right(user) else Left((StatusCode.Forbidden, ErrorMessage("user is not allowed to use this endpoint"))))
}

package com.example.auth

import com.example.auth.TapirAuthentication.TapirAuth
import com.example.errors._
import com.example.models.Roles.RoleType
import com.example.models.User
import com.example.utils.ZioUtil
import io.circe.generic.auto._
import sttp.model.StatusCode
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.PartialServerEndpoint
import sttp.tapir._
import zio.{ULayer, ZIO}

import scala.concurrent.Future

/**
 * Configures security endpoint.
 *
 * @param authentication authentication service.
 */
class TapirSecurity(authentication: ULayer[TapirAuth]) {

  /**
   * Creates secured endpoint with role restriction from argument. If role list is empty - authorization is disabled
   *
   * PartialServerEndpoint explained: [Security input, Security output, Input, Error response, Output,
   *   capabilities that are required by this endpoint's inputs/outputs, wrapper (in most cases - future)]
   * In security endpoint defined Security input - bearer token, security output - user,
   *    error response - tuple of status code with error message object and wrapper.
   */
  def tapirSecurityEndpoint(roles: List[RoleType]): PartialServerEndpoint[String, User, Unit, ErrorInfo, Unit, Any, Future] =
    endpoint // base tapir endpoint
      .securityIn(auth.bearer[String]().description("Bearer token from Authorization header")) // defining security input
      .errorOut(
        oneOf[ErrorInfo](
          // returns required http code for different types of ErrorInfo. For secured endpoint you need to define all cases before defining security logic
          oneOfVariant(statusCode(StatusCode.Forbidden).and(jsonBody[Forbidden].description("When user doesn't have role for the endpoint"))),
          oneOfVariant(statusCode(StatusCode.Unauthorized).and(jsonBody[Unauthorized].description("When user doesn't authenticated or token is expired"))),
          oneOfVariant(statusCode(StatusCode.NotFound).and(jsonBody[NotFound].description("When something not found"))),
          oneOfVariant(statusCode(StatusCode.BadRequest).and(jsonBody[BadRequest].description("Bad request"))),
          oneOfVariant(statusCode(StatusCode.InternalServerError).and(jsonBody[InternalServerError].description("For exceptional cases"))),
          // default case below.
          oneOfDefaultVariant(jsonBody[com.example.errors.ErrorMessage].description("Default result").example(com.example.errors.ErrorMessage("Test error message")))
        )
      )
      .serverSecurityLogic(token =>
        ZioUtil.foldRunToFuture(TapirAuthentication.authenticate(token).flatMap { user =>
          // define security logic here. For example, here is authentication, chained with authorization
          isAuthorized(user, roles)
        }.provide(authentication))
      )

  /**
   * Authorization filter function - checks user for present roles.
   * @param user user to check
   * @param roles restricted roles to check. If empty - skips authorization.
   * @return either error with Forbidden status code or user.
   */
  def isAuthorized(user: User, roles: List[RoleType]): ZIO[Any, ErrorInfo, User] =
    if (roles.isEmpty || roles.contains(user.role)) ZIO.succeed(user) else ZIO.fail(Forbidden("user is not allowed to use this endpoint"))
}

package com.example.auth

import com.example.errors.{BadRequest, ErrorInfo, Forbidden, InternalServerError, NotFound, Unauthorized}
import com.example.models.Roles.RoleType
import com.example.models.User
import com.example.utils.Util.foldEitherOfFuture
import sttp.model.StatusCode
import sttp.tapir.generic.auto._
import sttp.tapir.{auth, endpoint}
import sttp.tapir._
import io.circe.generic.auto._
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
  def isAuthorized(user: User, roles: List[RoleType]): Future[Either[ErrorInfo, User]] =
    Future.successful(if (roles.isEmpty || roles.contains(user.role)) Right(user) else Left(Forbidden("user is not allowed to use this endpoint")))
}

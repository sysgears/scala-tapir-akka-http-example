package com.example.controllers

import com.example.errors.{BadRequest, Conflict, ErrorInfo, ErrorMessage}
import com.example.models.Token
import com.example.models.forms.{SignInForm, SignUpForm}
import com.example.services.AuthService
import com.example.services.AuthService.Authentication
import com.example.utils.ZioUtil
import sttp.tapir.{endpoint, oneOf, oneOfDefaultVariant, oneOfVariant, statusCode}
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody
import io.circe.generic.auto._
import sttp.capabilities.WebSockets
import sttp.capabilities.akka.AkkaStreams
import sttp.model.StatusCode
import sttp.tapir.server.ServerEndpoint
import zio.ULayer

import scala.concurrent.{ExecutionContext, Future}

/**
 * Controller, which contains auth functions - sign in and sign up.
 *
 * @param authService service for the controller.
 */
class AuthController(authService: ULayer[Authentication]) {

  /**
   * Sign in endpoint defining.
   */
  val signInEndpoint = endpoint
    .post // POST endpoint
    .in("signIn") // /signIn uri
    .description("Sign in endpoint.")
    .in(jsonBody[SignInForm] // requires signInForm in request body, added description and example.
          .description("Required data to log in").example(SignInForm("test@example.com", "pass4567")))
    .out(jsonBody[Token].description("Bearer token for authorization header").example(Token("lkngla2pj45ij3oijma2oij..."))) // described response
    .errorOut(jsonBody[ErrorInfo]) // described error response type, will return string as json with http 400 code
    .serverLogic { form => // defining logic for the endpoint.
      ZioUtil.foldRunToFuture(AuthService.signIn(form).provide(authService))
    }

  /**
   * Sign up endpoint defining.
   */
  val signUpEndpoint = endpoint
    .post // POST endpoint
    .in("signUp") // /signUp defining.
    .description("Sign up endpoint.")
    .in(jsonBody[SignUpForm] // requires signUpForm in request body, added description and example.
          .description("Required data to sign up")
          .example(SignUpForm("test name", "+77777777777", "test@example.com", "49050", "Dnipro", "test address, 46", "pass456", "pass456")))
    .out(statusCode(StatusCode.Created).description("Returns Created when user is registered")) // defining static status code for success response.
    .errorOut(
      oneOf[ErrorInfo](
        oneOfVariant(statusCode(StatusCode.Conflict).and(jsonBody[Conflict].description("When something not found"))),
        oneOfVariant(statusCode(StatusCode.BadRequest).and(jsonBody[BadRequest].description("if body is incorrect, if passwords not matches or password is too short"))),
        oneOfDefaultVariant(jsonBody[com.example.errors.ErrorMessage].description("Default result").example(com.example.errors.ErrorMessage("Test error message")))
      )
    )
    .serverLogic { signUpForm => // defined logic for the endpoint.
      ZioUtil.foldRunToFuture(AuthService.signUp(signUpForm).provide(authService))
    }

  /** Convenient way to assemble endpoints from the controller and then concat this route to main route. */
  val authRoutes: List[ServerEndpoint[AkkaStreams with WebSockets, Future]] = List(signInEndpoint, signUpEndpoint)
}

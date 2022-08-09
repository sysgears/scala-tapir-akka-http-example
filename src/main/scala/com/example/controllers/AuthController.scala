package com.example.controllers

import akka.http.scaladsl.server.{Directives, Route}
import com.example.models.Token
import com.example.models.forms.{SignInForm, SignUpForm}
import com.example.services.AuthService
import sttp.tapir.{endpoint, statusCode}
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody
import io.circe.generic.auto._
import sttp.model.StatusCode
import sttp.tapir.server.akkahttp.AkkaHttpServerInterpreter

import scala.concurrent.ExecutionContext

/**
 * Controller, which contains auth functions - sign in and sign up.
 *
 * @param authService service for the controller.
 * @param ec for futures.
 */
class AuthController(authService: AuthService)(implicit ec: ExecutionContext) {

  /**
   * Sign in endpoint defining.
   */
  val signInEndpoint: Route = AkkaHttpServerInterpreter().toRoute(endpoint
    .post // POST endpoint
    .in("signIn") // /signIn uri
    .in(jsonBody[SignInForm] // requires signInForm in request body, added description and example.
          .description("Required data to log in").example(SignInForm("test@example.com", "pass4567")))
    .out(jsonBody[Token].description("Bearer token for authorization header").example(Token("Bearer lkngla2pj45ij3oijma2oij..."))) // described response
    .errorOut(jsonBody[String]) // described error response type, will return string as json with http 400 code
    .serverLogic { form => // defining logic for the endpoint.
      authService.signIn(form)
    })

  /**
   * Sign up endpoint defining.
   */
  val signUpEndpoint: Route = AkkaHttpServerInterpreter().toRoute(endpoint
    .post // POST endpoint
    .in("signUp") // /signUp defining.
    .in(jsonBody[SignUpForm] // requires signUpForm in request body, added description and example.
          .description("Required data to sign up")
          .example(SignUpForm("test name", "+77777777777", "test@example.com", "49050", "Dnipro", "test address, 46", "pass456", "pass456")))
    .out(statusCode(StatusCode.Created).description("Returns Created when user is registered")) // defining static status code for success response.
    .errorOut(statusCode.description("Returns 409 if user with that email is already exists, 400 if body is incorrect, if passwords not matches or password is too short"))
    // defined dynamic status code for error response.
    .errorOut(jsonBody[String].description("Contains reason of response")) // defining response type for error response.
    .serverLogic { signUpForm => // defined logic for the endpoint.
      authService.signUp(signUpForm)
    })

  /** Convenient way to assemble endpoints from the controller and then concat this route to main route. */
  val authRoutes: Route = Directives.concat(signInEndpoint, signUpEndpoint)
}

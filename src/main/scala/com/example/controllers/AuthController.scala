package com.example.controllers

import java.time.LocalDateTime

import akka.http.scaladsl.server.{Directives, Route}
import com.example.auth.JwtService
import com.example.dao.UserDao
import com.example.models.{Roles, Token, User}
import com.example.models.forms.{SignInForm, SignUpForm}
import com.example.utils.CryptUtils
import sttp.tapir.{endpoint, statusCode}
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody
import io.circe.generic.auto._
import sttp.model.StatusCode
import sttp.tapir.server.akkahttp.AkkaHttpServerInterpreter

import scala.concurrent.{ExecutionContext, Future}

/**
 * Controller, which contains auth functions - sign in and sign up.
 *
 * @param userDao user dao
 * @param jwtService jwt service
 * @param ec for futures.
 */
class AuthController(userDao: UserDao, jwtService: JwtService)(implicit ec: ExecutionContext) {

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
      userDao.findByEmail(form.login).map {
        case Some(user) =>
          if (CryptUtils.matchBcryptHash(form.password, user.passwordHash).getOrElse(false)) {
            val jwtToken = jwtService.generateJwt(user.id)
            Right(Token(jwtToken))
          } else {
            Left("Login or password is incorrect. Please, try again")
          }
        case None => Left("Login or password is incorrect. Please, try again")
      }
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
      if (!signUpForm.password.equals(signUpForm.repeatPassword)) {
        Future.successful(Left(StatusCode.BadRequest, "Passwords not matches!"))
      } else if (signUpForm.password.length < 6) {
        Future.successful(Left(StatusCode.BadRequest, "Password is too short!"))
      } else {
        userDao.findByEmail(signUpForm.email).flatMap {
          case Some(_) =>
            Future.successful(Left(StatusCode.Conflict, "User with this email is already exists"))
          case None =>
            userDao.createUser(User(0, signUpForm.name, signUpForm.phoneNumber, signUpForm.email,
              CryptUtils.createBcryptHash(signUpForm.password), signUpForm.zip, signUpForm.city,
              signUpForm.address, Roles.User, LocalDateTime.now)).map { _ =>
              Right(())
            }
        }
      }
    })

  /** Convenient way to assemble endpoints from the controller and then concat this route to main route. */
  val authRoutes: Route = Directives.concat(signInEndpoint, signUpEndpoint)
}

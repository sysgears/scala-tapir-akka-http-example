package com.example.controllers

import java.time.LocalDateTime

import akka.http.scaladsl.server.{Directives, Route}
import com.example.auth.TapirSecurity
import com.example.dao.UserDao
import com.example.models.{Token, User}
import com.example.models.forms.{SignInForm, SignUpForm}
import com.example.utils.{CryptUtils, JwtUtils}
import sttp.tapir.{endpoint, statusCode}
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody
import io.circe.generic.auto._
import sttp.model.StatusCode
import sttp.tapir.server.akkahttp.AkkaHttpServerInterpreter

import scala.concurrent.{ExecutionContext, Future}

class AuthController(tapirSecurity: TapirSecurity, userDao: UserDao, jwtUtils: JwtUtils)(implicit ec: ExecutionContext) {

  val signInEndpoint: Route = AkkaHttpServerInterpreter().toRoute(endpoint.post
    .in("signIn")
    .in(jsonBody[SignInForm]
          .description("Required data to log in").example(SignInForm("test@example.com", "pass4567")))
    .out(jsonBody[Token].description("Bearer token for authorization header").example(Token("Bearer lkngla2pj45ij3oijma2oij...")))
    .errorOut(jsonBody[String])
    .serverLogic { form =>
      userDao.findByEmail(form.login).map {
        case Some(user) =>
          if (CryptUtils.matchBcryptHash(form.password, user.passwordHash).getOrElse(false)) {
            val jwtToken = jwtUtils.generateJwt(user.id)
            Right(Token(jwtToken))
          } else {
            Left("Login or password is incorrect. Please, try again")
          }
        case None => Left("Login or password is incorrect. Please, try again")
      }
    })

  val signUpEndpoint: Route = AkkaHttpServerInterpreter().toRoute(endpoint.post
    .in("signUp")
    .in(jsonBody[SignUpForm]
          .description("Required data to sign up")
          .example(SignUpForm("test name", "+77777777777", "test@example.com", "49050", "Dnipro", "test address, 46", "pass456", "pass456")))
    .out(statusCode(StatusCode.Created).description("Returns Created when user is registered"))
    .errorOut(statusCode.description("Returns 409 if user with that email is already exists, 400 if body is incorrect, if passwords not matches or password is too short"))
    .errorOut(jsonBody[String].description("Contains reason of response"))
    .serverLogic { signUpForm =>
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
              signUpForm.address, "User", LocalDateTime.now)).map { _ =>
              Right(())
            }
        }
      }
    })

  val authRoutes: Route = Directives.concat(signInEndpoint, signUpEndpoint)
}

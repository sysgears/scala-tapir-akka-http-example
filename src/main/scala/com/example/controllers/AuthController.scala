package com.example.controllers

import com.example.auth.TapirSecurity
import com.example.dao.UserDao
import com.example.models.Token
import com.example.models.forms.SignInForm
import com.example.utils.{CryptUtils, JwtUtils}
import sttp.tapir.endpoint
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody
import io.circe.generic.auto._
import sttp.tapir.server.ServerEndpoint.Full

import scala.concurrent.{ExecutionContext, Future}

class AuthController(tapirSecurity: TapirSecurity, userDao: UserDao, jwtUtils: JwtUtils)(implicit ec: ExecutionContext) {

  val signInEndpoint: Full[Unit, Unit, SignInForm, String, Token, Any, Future] = endpoint.post
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
    }
}

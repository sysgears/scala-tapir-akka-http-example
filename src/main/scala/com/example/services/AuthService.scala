package com.example.services

import com.example.auth.Jwt.JwtService
import com.example.dao.UserDao.UserRepository
import com.example.errors._
import com.example.models.forms.{SignInForm, SignUpForm}
import com.example.models.{Roles, Token, User}
import com.example.services
import com.example.services.AuthService.Authentication
import com.example.utils.{CryptUtils, Util}
import com.typesafe.scalalogging.LazyLogging
import zio.{ZIO, ZLayer}

import java.time.LocalDateTime

/** Service for the AuthController.
  *
  * Contains functions, required for the controller's endpoints.
  */
object AuthService {

  type Authentication = Service

  trait Service {

    /** Signs in user.
      *
      * @param form contains login and password for sign in.
      * @return either error message or token class with jwt token.
      */
    def signIn(form: SignInForm): ZIO[Any, ErrorMessage, Token]

    /** Registers user.
      *
      * Id for the new user is created here to keep determinism for database
      *
      * @param signUpForm contains data for new user registration.
      * @return either error message inside required response class or just signal to return positive response.
      */
    def signUp(signUpForm: SignUpForm): ZIO[Any, ErrorInfo, Unit]
  }

  def signIn(form: SignInForm): ZIO[Authentication, ErrorMessage, Token] =
    ZIO.serviceWithZIO[Authentication](_.signIn(form))

  def signUp(form: SignUpForm): ZIO[Authentication, ErrorInfo, Unit] =
    ZIO.serviceWithZIO[Authentication](_.signUp(form))

  val live: ZLayer[JwtService with UserRepository, Nothing, services.AuthService.Authentication] = ZLayer {
    for {
      userDao    <- ZIO.service[UserRepository]
      jwtService <- ZIO.service[JwtService]
    } yield {
      new AuthService(userDao, jwtService)
    }
  }
}

class AuthService(userDao: UserRepository, jwtService: JwtService) extends Authentication with LazyLogging {

  override def signIn(form: SignInForm): ZIO[Any, ErrorMessage, Token] =
    userDao
      .findByEmail(form.login)
      .mapError { error =>
        logger.error("Intercepted error from sign in action", error)
        ErrorMessage("Internal error")
      }
      .flatMap {
        case Some(user) =>
          if (
            CryptUtils
              .matchBcryptHash(form.password, user.passwordHash)
              .getOrElse(false)
          ) {
            logger.debug(s"User with id ${user.id} has logged in")
            jwtService.generateJwt(user.id).map(Token(_))
          } else {
            ZIO.fail(
              ErrorMessage(
                "Login or password is incorrect. Please, try again"
              )
            )
          }
        case None =>
          ZIO.fail(
            ErrorMessage(
              "Login or password is incorrect. Please, try again"
            )
          )
      }

  def signUp(signUpForm: SignUpForm): ZIO[Any, ErrorInfo, Unit] = {
    val isValid = signUpForm.isValid // sign up form validation
    isValid match {
      case Left(message) => ZIO.fail(BadRequest(message))
      case Right(_) =>
        userDao
          .findByEmail(signUpForm.email)
          .flatMap {
            case Some(_) =>
              ZIO.fail(Conflict("User with this email already exists"))
            case None =>
              val newUser = User(
                Util.generateUuid,
                signUpForm.name,
                signUpForm.phoneNumber,
                signUpForm.email,
                CryptUtils.createBcryptHash(signUpForm.password),
                signUpForm.zip,
                signUpForm.city,
                signUpForm.address,
                Roles.User,
                LocalDateTime.now
              )
              userDao.createUser(newUser).map { _ =>
                logger.debug(
                  s"User with email ${newUser.email} has been registered."
                )
                ()
              }
          }
          .mapError {
            case error: ErrorInfo => error // pass this one
            case error =>
              logger.error("Intercepted error from sign up action", error)
              InternalServerError("Internal error")
          }
    }
  }
}

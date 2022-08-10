package com.example.services

import java.time.LocalDateTime

import com.example.auth.JwtService
import com.example.dao.UserDao
import com.example.errors.{BadRequest, Conflict, ErrorInfo, ErrorMessage}
import com.example.models.{Roles, Token, User}
import com.example.models.forms.{SignInForm, SignUpForm}
import com.example.utils.CryptUtils
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{ExecutionContext, Future}

/**
 * Service for the AuthController.
 *
 * Contains functions, required for the controller's endpoints.
 *
 * @param userDao dao for users.
 * @param jwtService contains functions, which works with jwt token.
 * @param ec for futures.
 */
class AuthService(userDao: UserDao, jwtService: JwtService)(implicit ec: ExecutionContext) extends LazyLogging {

  /**
   * Signs in user.
   * @param form contains login and password for sign in.
   * @return either error message or token class with jwt token.
   */
  def signIn(form: SignInForm): Future[Either[ErrorMessage, Token]] = {
    userDao.findByEmail(form.login).map {
      case Some(user) =>
        if (CryptUtils.matchBcryptHash(form.password, user.passwordHash).getOrElse(false)) {
          logger.debug(s"User with id ${user.id} has logged in")
          val jwtToken = jwtService.generateJwt(user.id)
          Right(Token(jwtToken))
        } else {
          Left(ErrorMessage("Login or password is incorrect. Please, try again"))
        }
      case None => Left(ErrorMessage("Login or password is incorrect. Please, try again"))
    }
  }

  /**
   * Registers user.
   *
   * @param signUpForm contains data for new user registration.
   * @return either error message inside required response class or just signal to return positive response.
   */
  def signUp(signUpForm: SignUpForm): Future[Either[ErrorInfo, Unit]] = {
    val isValid = signUpForm.isValid // sign up form validation
    isValid match {
      case Left(message) => Future.successful(Left(BadRequest(message)))
      case Right(_) =>
        userDao.findByEmail(signUpForm.email).flatMap {
          case Some(_) =>
            Future.successful(Left(Conflict("User with this email is already exists")))
          case None =>
            val newUser = User(0, signUpForm.name, signUpForm.phoneNumber, signUpForm.email,
              CryptUtils.createBcryptHash(signUpForm.password), signUpForm.zip, signUpForm.city,
              signUpForm.address, Roles.User, LocalDateTime.now)
            userDao.createUser(newUser).map { _ =>
              logger.debug(s"User with email ${newUser.email} has registered.")
              Right(())
            }
        }
    }
  }

}

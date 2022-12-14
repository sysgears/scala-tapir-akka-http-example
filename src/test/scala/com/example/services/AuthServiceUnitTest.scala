package com.example.services

import java.time.LocalDateTime

import com.example.auth.JwtService
import com.example.dao.UserDao
import com.example.errors.{BadRequest, Conflict}
import com.example.models.forms.{SignInForm, SignUpForm}
import com.example.models.{Roles, User}
import org.mockito.ArgumentMatchers.{any, anyLong}
import org.mockito.Mockito.when
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar.mock

import scala.concurrent.Future

/** Tests for auth service to check if implementation works as intended. */
class AuthServiceUnitTest extends AsyncFlatSpec with Matchers {

  val testUser: User = User(1, "test name", "+777777777", "test@example.com", "$2a$10$XMuxo.3xlVlGnySYCUOOIOl09n6olXw7aV2daE9Ief.Gd/js.Fq1O", "49050", "Dnipro", "test address", Roles.User, LocalDateTime.now())

  /** Case when signIn function received correct form. */
  it should "comparing correct user correctly" in {
    // preparations
    val userDao = mock[UserDao]
    val jwtService = mock[JwtService]
    val authService = new AuthService(userDao, jwtService)

    //given
    when(userDao.findByEmail(any[String])).thenReturn(Future.successful(Some(testUser)))
    when(jwtService.generateJwt(anyLong())).thenReturn("token")
    val signInForm = SignInForm("test@example.com", "password")

    // when
    val signInResult = authService.signIn(signInForm)

    //then
    signInResult.map { either =>
      either.isRight shouldBe true
      either.toOption.get.token shouldBe "token"
    }
  }

  /** Case when passwords not matches. */
  it should "comparing incorrect user correctly" in {
    // preparations
    val userDao = mock[UserDao]
    val jwtService = mock[JwtService]
    val authService = new AuthService(userDao, jwtService)

    //given
    when(userDao.findByEmail(any[String])).thenReturn(Future.successful(Some(testUser)))
    when(jwtService.generateJwt(anyLong())).thenReturn("token")
    val signInForm = SignInForm("test@example.com", "password1")

    // when
    val signInResult = authService.signIn(signInForm)

    //then
    signInResult.map { either =>
      either.isLeft shouldBe true
      either.left.toOption.get.msg shouldBe "Login or password is incorrect. Please, try again"
    }
  }

  /** Case for login when user not found for the email. */
  it should "return correct result if user not found" in {
    // preparations
    val userDao = mock[UserDao]
    val jwtService = mock[JwtService]
    val authService = new AuthService(userDao, jwtService)

    //given
    when(userDao.findByEmail(any[String])).thenReturn(Future.successful(None))
    val signInForm = SignInForm("test@example.com", "password1")

    // when
    val signInResult = authService.signIn(signInForm)

    //then
    signInResult.map { either =>
      either.isLeft shouldBe true
      either.left.toOption.get.msg shouldBe "Login or password is incorrect. Please, try again"
    }
  }

  /** Case when registration form is correct. */
  it should "register new user correctly" in {
    // preparations
    val userDao = mock[UserDao]
    val jwtService = mock[JwtService]
    val authService = new AuthService(userDao, jwtService)

    //given
    when(userDao.findByEmail(any[String])).thenReturn(Future.successful(None))
    when(userDao.createUser(any[User])).thenReturn(Future.successful(1))
    val signUpForm = SignUpForm("test name", "+77777777777", "test@example.com", "49050", "Dnipro", "test address, 46", "pass456", "pass456")

    // when
    val signUpResult = authService.signUp(signUpForm)

    //then
    signUpResult.map { either =>
      either.isRight shouldBe true
    }
  }

  /** Case for registration when user with that email exists.  */
  it should "return conflict for registration new user" in {
    // preparations
    val userDao = mock[UserDao]
    val jwtService = mock[JwtService]
    val authService = new AuthService(userDao, jwtService)

    //given
    when(userDao.findByEmail(any[String])).thenReturn(Future.successful(Some(testUser)))
    when(userDao.createUser(any[User])).thenReturn(Future.successful(1))
    val signUpForm = SignUpForm("test name", "+77777777777", "test@example.com", "49050", "Dnipro", "test address, 46", "pass456", "pass456")

    // when
    val signUpResult = authService.signUp(signUpForm)

    //then
    signUpResult.map { either =>
      either.isLeft shouldBe true
      either.left.toOption.get.isInstanceOf[Conflict] shouldBe true
    }
  }

  /** Case for registration when registration form is invalid. */
  it should "return badRequest when registration form is invalid" in {
    // preparations
    val userDao = mock[UserDao]
    val jwtService = mock[JwtService]
    val authService = new AuthService(userDao, jwtService)

    //given
    when(userDao.findByEmail(any[String])).thenReturn(Future.successful(Some(testUser)))
    when(userDao.createUser(any[User])).thenReturn(Future.successful(1))
    val signUpForm = SignUpForm("test name", "+77777777777", "test@example.com", "49050", "Dnipro", "test address, 46", "pass456", "pass4567")

    // when
    val signUpResult = authService.signUp(signUpForm)

    //then
    signUpResult.map { either =>
      either.isLeft shouldBe true
      either.left.toOption.get.isInstanceOf[BadRequest] shouldBe true
    }
  }

}

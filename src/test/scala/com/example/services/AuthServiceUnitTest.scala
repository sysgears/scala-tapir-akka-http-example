package com.example.services

import com.example.auth.Jwt.JwtService
import com.example.dao.UserDao.UserRepository
import com.example.errors.ErrorInfo
import com.example.models.forms.{SignInForm, SignUpForm}
import com.example.models.{Roles, User}
import com.example.services.AuthService.Authentication
import com.example.utils.Util
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import zio._
import zio.test.Assertion._
import zio.test._

import java.time.LocalDateTime

/** Tests for auth service to check if implementation works as intended. */
object AuthServiceUnitTest extends ZIOSpecDefault {

  val testUser: User = User(
    Util.generateUuid,
    "test name",
    "+777777777",
    "test@example.com",
    "$2a$10$XMuxo.3xlVlGnySYCUOOIOl09n6olXw7aV2daE9Ief.Gd/js.Fq1O",
    "49050",
    "Dnipro",
    "test address",
    Roles.User,
    LocalDateTime.now()
  )

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("AuthService")(
      /** Case when signIn function received correct form. */
      test("comparing correct user correctly") {
        // preparations
        val userDao = mock[UserRepository]
        val jwtService = mock[JwtService]
        val authService = AuthService.live

        //given
        when(userDao.findByEmail(any[String]))
          .thenReturn(ZIO.succeed(Some(testUser)))
        when(jwtService.generateJwt(anyString()))
          .thenReturn(ZIO.succeed("token"))
        val signInForm = SignInForm("test@example.com", "password")
        for {
          signInResult <- ZIO
            .serviceWithZIO[Authentication](_.signIn(signInForm))
            .provide(
              authService,
              ZLayer.succeed(userDao),
              ZLayer.succeed(jwtService)
            )
        } yield {
          assertTrue(signInResult.token == "token")
        }
      },
      test("comparing incorrect user correctly") {
        // preparations
        val userDao = mock[UserRepository]
        val jwtService = mock[JwtService]
        val authService = AuthService.live

        //given
        when(userDao.findByEmail(any[String]))
          .thenReturn(ZIO.succeed(Some(testUser)))
        when(jwtService.generateJwt(anyString()))
          .thenReturn(ZIO.succeed("token"))
        val signInForm = SignInForm("test@example.com", "password1")

        assertZIO(
          ZIO
            .serviceWithZIO[Authentication](_.signIn(signInForm))
            .provide(
              authService,
              ZLayer.succeed(userDao),
              ZLayer.succeed(jwtService)
            )
            .exit
        )(fails(isSubtype[ErrorInfo](anything)))
      },
      test("return correct result if user not found") {
        // preparations
        val userDao = mock[UserRepository]
        val jwtService = mock[JwtService]
        val authService = AuthService.live

        //given
        when(userDao.findByEmail(any[String])).thenReturn(ZIO.succeed(None))
        val signInForm = SignInForm("test@example.com", "password1")

        // assert
        assertZIO(
          ZIO
            .serviceWithZIO[Authentication](_.signIn(signInForm))
            .provide(
              authService,
              ZLayer.succeed(userDao),
              ZLayer.succeed(jwtService)
            )
            .exit
        )(fails(isSubtype[ErrorInfo](anything)))
      },
      /** Case when registration form is correct. */
      test("register new user correctly") {
        // preparations
        val userDao = mock[UserRepository]
        val jwtService = mock[JwtService]
        val authService = AuthService.live

        //given
        when(userDao.findByEmail(any[String])).thenReturn(ZIO.succeed(None))
        when(userDao.createUser(any[User])).thenReturn(ZIO.succeed(1L))
        val signUpForm = SignUpForm(
          "test name",
          "+77777777777",
          "test@example.com",
          "49050",
          "Dnipro",
          "test address, 46",
          "pass456",
          "pass456"
        )

        for {
          signUpResult <- ZIO
            .serviceWithZIO[Authentication](_.signUp(signUpForm))
            .provide(
              authService,
              ZLayer.succeed(userDao),
              ZLayer.succeed(jwtService)
            )
        } yield {
          assertTrue(true)
        }
      },
      /** Case for registration when user with that email exists.  */
      test("return conflict for registration new user") {
        // preparations
        val userDao = mock[UserRepository]
        val jwtService = mock[JwtService]
        val authService = AuthService.live

        //given
        when(userDao.findByEmail(any[String]))
          .thenReturn(ZIO.succeed(Some(testUser)))
        when(userDao.createUser(any[User])).thenReturn(ZIO.succeed(1L))
        val signUpForm = SignUpForm(
          "test name",
          "+77777777777",
          "test@example.com",
          "49050",
          "Dnipro",
          "test address, 46",
          "pass456",
          "pass456"
        )

        // assert
        assertZIO(
          ZIO
            .serviceWithZIO[Authentication](_.signUp(signUpForm))
            .provide(
              authService,
              ZLayer.succeed(userDao),
              ZLayer.succeed(jwtService)
            )
            .exit
        )(fails(isSubtype[ErrorInfo](anything)))
      },
      test("return badRequest when registration form is invalid") {
        // preparations
        val userDao = mock[UserRepository]
        val jwtService = mock[JwtService]
        val authService = AuthService.live

        //given
        when(userDao.findByEmail(any[String]))
          .thenReturn(ZIO.succeed(Some(testUser)))
        when(userDao.createUser(any[User])).thenReturn(ZIO.succeed(1L))
        val signUpForm = SignUpForm(
          "test name",
          "+77777777777",
          "test@example.com",
          "49050",
          "Dnipro",
          "test address, 46",
          "pass456",
          "pass4567"
        )
        // assert
        assertZIO(
          ZIO
            .serviceWithZIO[Authentication](_.signUp(signUpForm))
            .provide(
              authService,
              ZLayer.succeed(userDao),
              ZLayer.succeed(jwtService)
            )
            .exit
        )(fails(isSubtype[ErrorInfo](anything)))
      }
    )
}

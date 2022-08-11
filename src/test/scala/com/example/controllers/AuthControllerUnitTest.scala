package com.example.controllers

import com.example.errors.{BadRequest, Conflict, ErrorMessage}
import com.example.models.Token
import com.example.models.forms.{SignInForm, SignUpForm}
import com.example.services.AuthService
import io.circe.syntax.EncoderOps
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar.mock
import sttp.client3._
import io.circe.generic.auto._
import sttp.client3.testing.SttpBackendStub
import sttp.model.StatusCode
import sttp.tapir.server.stub.TapirStubInterpreter

import scala.concurrent.Future

class AuthControllerUnitTest extends AsyncFlatSpec with Matchers {

  it should "log in correctly" in {
    val authService = mock[AuthService]
    when(authService.signIn(any[SignInForm])).thenReturn(Future(Right(Token("password"))))
    val authController = new AuthController(authService)
    // given
    val backendStub: SttpBackend[Future, Any] = TapirStubInterpreter(SttpBackendStub.asynchronousFuture)
      .whenServerEndpoint(authController.signInEndpoint)
      .thenRunLogic()
      .backend()

    // when
    val response = basicRequest
      .post(uri"http://localhost:9000/signIn")
      .body(SignInForm("test@example.com", "password").asJson.noSpaces)
      .send(backendStub)

    // then
    response.map(x => println(s"signIn expecting Token message body: ${x.body}"))
    response.map(_.body shouldBe Right(Token("password").asJson.noSpaces))
  }

  it should "return badRequest when service returns failure result" in {
    val authService = mock[AuthService]
    when(authService.signIn(any[SignInForm])).thenReturn(Future(Left(ErrorMessage("Login or password is incorrect. Please, try again"))))
    val authController = new AuthController(authService)
    // given
    val backendStub: SttpBackend[Future, Any] = TapirStubInterpreter(SttpBackendStub.asynchronousFuture)
      .whenServerEndpoint(authController.signInEndpoint)
      .thenRunLogic()
      .backend()

    // when
    val response = basicRequest
      .post(uri"http://localhost:9000/signIn")
      .body(SignInForm("test@example.com", "password").asJson.noSpaces)
      .send(backendStub)

    // then
    response.map { resp =>
      println(s"signIn expecting BadRequest message body: ${resp.body}")
      resp.code shouldBe StatusCode.BadRequest
      resp.body shouldBe Left(ErrorMessage("Login or password is incorrect. Please, try again").asJson.noSpaces)
    }
  }

  it should "return conflict for sign up when service returns conflict errorInfo" in {
    val authService = mock[AuthService]
    when(authService.signUp(any[SignUpForm])).thenReturn(Future.successful(Left(Conflict("User with this email is already exists"))))
    val authController = new AuthController(authService)
    // given
    val backendStub: SttpBackend[Future, Any] = TapirStubInterpreter(SttpBackendStub.asynchronousFuture)
      .whenServerEndpoint(authController.signUpEndpoint)
      .thenRunLogic()
      .backend()

    // when
    val response = basicRequest
      .post(uri"http://localhost:9000/signUp")
      .body(SignUpForm("test name", "+77777777777", "test@example.com", "49050", "Dnipro", "test address, 46", "pass456", "pass456").asJson.noSpaces)
      .send(backendStub)

    // then
    response.map { resp =>
      println(s"signUp expecting Conflict message body: ${resp.body}")
      resp.code shouldBe StatusCode.Conflict
      resp.body shouldBe Left(Conflict("User with this email is already exists").asJson.noSpaces)
    }
  }

  it should "return badRequest for sign up when service returns badRequest errorInfo" in {
    val authService = mock[AuthService]
    when(authService.signUp(any[SignUpForm])).thenReturn(Future.successful(Left(BadRequest("BadRequest message"))))
    val authController = new AuthController(authService)
    // given
    val backendStub: SttpBackend[Future, Any] = TapirStubInterpreter(SttpBackendStub.asynchronousFuture)
      .whenServerEndpoint(authController.signUpEndpoint)
      .thenRunLogic()
      .backend()

    // when
    val response = basicRequest
      .post(uri"http://localhost:9000/signUp")
      .body(SignUpForm("test name", "+77777777777", "test@example.com", "49050", "Dnipro", "test address, 46", "pass456", "pass456").asJson.noSpaces)
      .send(backendStub)

    // then
    response.map { resp =>
      println(s"signUp expecting BadRequest message body: ${resp.body}")
      resp.code shouldBe StatusCode.BadRequest
      resp.body shouldBe Left(BadRequest("BadRequest message").asJson.noSpaces)
    }
  }

  it should "return Created when everything is okay" in {
    val authService = mock[AuthService]
    when(authService.signUp(any[SignUpForm])).thenReturn(Future.successful(Right()))
    val authController = new AuthController(authService)
    // given
    val backendStub: SttpBackend[Future, Any] = TapirStubInterpreter(SttpBackendStub.asynchronousFuture)
      .whenServerEndpoint(authController.signUpEndpoint)
      .thenRunLogic()
      .backend()

    // when
    val response = basicRequest
      .post(uri"http://localhost:9000/signUp")
      .body(SignUpForm("test name", "+77777777777", "test@example.com", "49050", "Dnipro", "test address, 46", "pass456", "pass456").asJson.noSpaces)
      .send(backendStub)

    // then
    response.map { resp =>
      resp.code shouldBe StatusCode.Created
    }
  }
}

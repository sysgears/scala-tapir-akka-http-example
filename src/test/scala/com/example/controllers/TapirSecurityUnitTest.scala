package com.example.controllers

import java.time.LocalDateTime

import com.example.auth.{TapirAuthentication, TapirSecurity}
import com.example.errors.{Forbidden, Unauthorized}
import com.example.models.{Roles, User}
import com.example.services.OrderService
import com.typesafe.scalalogging.LazyLogging
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

/**
 * Contains tests for endpoint security. Only TapirSecurity is checked.
 *
 * OrdersController is used because using ready to go endpoint is faster.
 *
 * Only invalid cases are present, because other tests using this security will use success auth case.
 */
class TapirSecurityUnitTest extends AsyncFlatSpec with Matchers with LazyLogging {

  val testUser: User = User(1, "test name", "+777777777", "test@example.com", "hash", "49050", "Dnipro", "test address", Roles.User, LocalDateTime.now())

  /** Case where user with wrong role is trying get endpoint for another user role. */
  it should "Reject user with wrong role" in {
    // preparations
    val authentication = mock[TapirAuthentication]
    when(authentication.authenticate(any[String])).thenReturn(Future.successful(Right(testUser.copy(role = Roles.Admin))))
    val orderService = mock[OrderService]
    val orderController = new OrderController(new TapirSecurity(authentication), orderService)

    // given
    val backendStub: SttpBackend[Future, Any] = TapirStubInterpreter(SttpBackendStub.asynchronousFuture)
      .whenServerEndpoint(orderController.viewUserOrderListEndpoint)
      .thenRunLogic()
      .backend()

    // when
    val response = basicRequest
      .get(uri"http://localhost:9000/orders")
      .header("Authorization", "Bearer password")
      .send(backendStub)

    // then
    response.map { resp =>
      logger.info(s"orders expecting 403 Forbidden: ${resp.body}")
      resp.code shouldBe StatusCode.Forbidden
      resp.body shouldBe Left(Forbidden("user is not allowed to use this endpoint").asJson.noSpaces)
    }
  }

  /** Case when user trying access endpoint with invalid jwt token. */
  it should "Reject user with wrong or expired jwt token" in {
    // preparations
    val authentication = mock[TapirAuthentication]
    when(authentication.authenticate(any[String])).thenReturn(Future.successful(Left(Unauthorized("Token is expired. You need to log in first"))))
    val orderService = mock[OrderService]
    val orderController = new OrderController(new TapirSecurity(authentication), orderService)

    // given
    val backendStub: SttpBackend[Future, Any] = TapirStubInterpreter(SttpBackendStub.asynchronousFuture)
      .whenServerEndpoint(orderController.viewUserOrderListEndpoint)
      .thenRunLogic()
      .backend()

    // when
    val response = basicRequest
      .get(uri"http://localhost:9000/orders")
      .header("Authorization", "Bearer password")
      .send(backendStub)

    // then
    response.map { resp =>
      println(s"orders expecting 401 Unauthorized when jwt token is invalid: ${resp.body}")
      resp.code shouldBe StatusCode.Unauthorized
      resp.body shouldBe Left(Unauthorized("Token is expired. You need to log in first").asJson.noSpaces)
    }
  }

  /** Case where user is trying access to endpoint without jwt token in header. */
  it should "Reject user without jwt token" in {
    // preparations
    val authentication = mock[TapirAuthentication]
    val orderService = mock[OrderService]
    val orderController = new OrderController(new TapirSecurity(authentication), orderService)

    // given
    val backendStub: SttpBackend[Future, Any] = TapirStubInterpreter(SttpBackendStub.asynchronousFuture)
      .whenServerEndpoint(orderController.viewUserOrderListEndpoint)
      .thenRunLogic()
      .backend()

    // when
    val response = basicRequest
      .get(uri"http://localhost:9000/orders")
      .send(backendStub)

    // then
    response.map { resp =>
      logger.info(s"orders expecting 401 Unauthorized when header is missing: ${resp.body}")
      resp.code shouldBe StatusCode.Unauthorized
    }
  }
}

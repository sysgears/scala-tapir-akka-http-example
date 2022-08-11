package com.example.controllers

import java.time.LocalDateTime

import com.example.auth.{TapirAuthentication, TapirSecurity}
import com.example.errors.BadRequest
import com.example.models.forms.{CreateOrderForm, OrderProductForm}
import com.example.models.{Order, OrderRecord, OrderWithRecords, Product, Roles, User}
import com.example.services.OrderService
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
 * Contains example of mocking authentication.
 */
class OrderControllerUnitTest extends AsyncFlatSpec with Matchers {
  val testUser: User = User(1, "test name", "+777777777", "test@example.com", "hash", "49050", "Dnipro", "test address", Roles.User, LocalDateTime.now())

  val authentication: TapirAuthentication = mock[TapirAuthentication]
  when(authentication.authenticate(any[String])).thenReturn(Future.successful(Right(testUser)))

  it should "Return order list for user" in {
    // preparations
    val orderService = mock[OrderService]
    val orderList = List(Order(1, 1, LocalDateTime.now(), Order.NEW_STATUS, LocalDateTime.now(), "comment"))
    when(orderService.findOrdersForUser(1)).thenReturn(Future.successful(orderList))
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
      println(s"orders expecting Order list message body: ${resp.body}")
      resp.body shouldBe Right(orderList.asJson.noSpaces)
    }
  }

  it should "Return order details" in {
    // preparations
    val orderService = mock[OrderService]
    val orderResponse = OrderWithRecords(Order(1, 1, LocalDateTime.now(), Order.NEW_STATUS, LocalDateTime.now(), "comment"),
      List(OrderRecord(1, 1, Some(Product(1, "test product", "test description", 5.0)), 2)))
    when(orderService.getOrderDetails(1)).thenReturn(Future.successful(Some(orderResponse)))
    val orderController = new OrderController(new TapirSecurity(authentication), orderService)

    // given
    val backendStub: SttpBackend[Future, Any] = TapirStubInterpreter(SttpBackendStub.asynchronousFuture)
      .whenServerEndpoint(orderController.viewUserOrderEndpoint)
      .thenRunLogic()
      .backend()

    // when
    val response = basicRequest
      .get(uri"http://localhost:9000/orders/1")
      .header("Authorization", "Bearer password")
      .send(backendStub)

    // then
    response.map { resp =>
      println(s"orders expecting Order details message body: ${resp.body}")
      resp.body shouldBe Right(orderResponse.asJson.noSpaces)
    }
  }

  it should "Return NotFound to order details request for not-existing order" in {
    // preparations
    val orderService = mock[OrderService]
    when(orderService.getOrderDetails(2)).thenReturn(Future.successful(None))
    val orderController = new OrderController(new TapirSecurity(authentication), orderService)

    // given
    val backendStub: SttpBackend[Future, Any] = TapirStubInterpreter(SttpBackendStub.asynchronousFuture)
      .whenServerEndpoint(orderController.viewUserOrderEndpoint)
      .thenRunLogic()
      .backend()

    // when
    val response = basicRequest
      .get(uri"http://localhost:9000/orders/2")
      .header("Authorization", "Bearer password")
      .send(backendStub)

    // then
    response.map { resp =>
      println(s"orders expecting Order details NotFound message body: ${resp.body}")
      resp.code shouldBe StatusCode.NotFound
    }
  }

  it should "Create new order for the user" in {
    // preparations
    val orderService = mock[OrderService]
    when(orderService.createOrder(any[Long], any[CreateOrderForm])).thenReturn(Future.successful(List(1,2)))
    val orderController = new OrderController(new TapirSecurity(authentication), orderService)

    // given
    val backendStub: SttpBackend[Future, Any] = TapirStubInterpreter(SttpBackendStub.asynchronousFuture)
      .whenServerEndpoint(orderController.createOrderEndpoint)
      .thenRunLogic()
      .backend()

    // when
    val response = basicRequest
      .post(uri"http://localhost:9000/orders")
      .body(CreateOrderForm(List(OrderProductForm(1, 5)), "Some delivery comment").asJson.noSpaces)
      .header("Authorization", "Bearer password")
      .send(backendStub)

    // then
    response.map { resp =>
      println(s"orders expected creating new order with Created http response")
      resp.code shouldBe StatusCode.Created
    }
  }

  it should "Reject creating new user because some value about product is invalid" in {
    // preparations
    val orderService = mock[OrderService]
    when(orderService.createOrder(any[Long], any[CreateOrderForm])).thenReturn(Future.successful(List(1, 2)))
    val orderController = new OrderController(new TapirSecurity(authentication), orderService)

    // given
    val backendStub: SttpBackend[Future, Any] = TapirStubInterpreter(SttpBackendStub.asynchronousFuture)
      .whenServerEndpoint(orderController.createOrderEndpoint)
      .thenRunLogic()
      .backend()

    // when
    val response = basicRequest
      .post(uri"http://localhost:9000/orders")
      .body(CreateOrderForm(List(OrderProductForm(-1, 5)), "Some delivery comment").asJson.noSpaces)
      .header("Authorization", "Bearer password")
      .send(backendStub)

    // then
    response.map { resp =>
      println(s"orders expected creating new order with Created http response")
      resp.code shouldBe StatusCode.BadRequest
      resp.body shouldBe Left(BadRequest("Some order record contains invalid value!").asJson.noSpaces)
    }
  }

}

package com.example.controllers

import com.example.auth.TapirAuthentication.TapirAuth

import java.time.LocalDateTime
import com.example.auth.{TapirAuthentication, TapirSecurity}
import com.example.errors.{BadRequest, NotFound}
import com.example.models.forms.{CreateOrderForm, OrderProductForm}
import com.example.models.{
  Order,
  OrderRecord,
  OrderWithRecords,
  Product,
  Roles,
  User
}
import com.example.services.OrderService
import com.example.services.OrderService.OrdersService
import com.example.utils.Util
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
import zio.{ZIO, ZLayer}

import scala.concurrent.Future

/**
  * Contains example of mocking authentication.
  */
class OrderControllerUnitTest
    extends AsyncFlatSpec
    with Matchers
    with LazyLogging {
  val testUser: User = User(
    Util.generateUuid,
    "test name",
    "+777777777",
    "test@example.com",
    "hash",
    "49050",
    "Dnipro",
    "test address",
    Roles.User,
    LocalDateTime.now()
  )

  val authentication: TapirAuth = mock[TapirAuth]
  when(authentication.authenticate(any[String]))
    .thenReturn(ZIO.succeed(testUser))

  it should "Return order list for user" in {
    // preparations
    val orderService = mock[OrdersService]
    val orderList = List(
      Order(
        Util.generateUuid,
        testUser.id,
        LocalDateTime.now(),
        Order.NEW_STATUS,
        LocalDateTime.now(),
        "comment"
      )
    )
    when(orderService.findOrdersForUser(testUser.id))
      .thenReturn(ZIO.succeed(orderList))
    val orderController = new OrderController(
      new TapirSecurity(ZLayer.succeed(authentication)),
      ZLayer.succeed(orderService)
    )

    // given
    val backendStub: SttpBackend[Future, Any] =
      TapirStubInterpreter(SttpBackendStub.asynchronousFuture)
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
      logger.info(s"orders expecting Order list message body: ${resp.body}")
      resp.body shouldBe Right(orderList.asJson.noSpaces)
    }
  }

  it should "Return order details" in {
    // preparations
    val orderService = mock[OrdersService]
    val orderId = Util.generateUuid
    val orderResponse = OrderWithRecords(
      Order(
        orderId,
        testUser.id,
        LocalDateTime.now(),
        Order.NEW_STATUS,
        LocalDateTime.now(),
        "comment"
      ),
      List(
        OrderRecord(
          Some(
            Product(Util.generateUuid, "test product", "test description", 5.0)
          ),
          2
        )
      )
    )
    when(orderService.getOrderDetails(orderId))
      .thenReturn(ZIO.succeed(orderResponse))
    val orderController = new OrderController(
      new TapirSecurity(ZLayer.succeed(authentication)),
      ZLayer.succeed(orderService)
    )

    // given
    val backendStub: SttpBackend[Future, Any] =
      TapirStubInterpreter(SttpBackendStub.asynchronousFuture)
        .whenServerEndpoint(orderController.viewUserOrderEndpoint)
        .thenRunLogic()
        .backend()

    // when
    val response = basicRequest
      .get(uri"http://localhost:9000/orders/$orderId")
      .header("Authorization", "Bearer password")
      .send(backendStub)

    // then
    response.map { resp =>
      logger.info(s"orders expecting Order details message body: ${resp.body}")
      resp.body shouldBe Right(orderResponse.asJson.noSpaces)
    }
  }

  it should "Return NotFound to order details request for not-existing order" in {
    // preparations
    val orderService = mock[OrdersService]
    val orderId = Util.generateUuid
    when(orderService.getOrderDetails(orderId)).thenReturn(ZIO.fail(NotFound()))
    val orderController = new OrderController(
      new TapirSecurity(ZLayer.succeed(authentication)),
      ZLayer.succeed(orderService)
    )

    // given
    val backendStub: SttpBackend[Future, Any] =
      TapirStubInterpreter(SttpBackendStub.asynchronousFuture)
        .whenServerEndpoint(orderController.viewUserOrderEndpoint)
        .thenRunLogic()
        .backend()

    // when
    val response = basicRequest
      .get(uri"http://localhost:9000/orders/$orderId")
      .header("Authorization", "Bearer password")
      .send(backendStub)

    // then
    response.map { resp =>
      logger.info(
        s"orders expecting Order details NotFound message body: ${resp.body}"
      )
      resp.code shouldBe StatusCode.NotFound
    }
  }

  it should "Create new order for the user" in {
    // preparations
    val orderService = mock[OrdersService]
    when(orderService.createOrder(any[String], any[CreateOrderForm]))
      .thenReturn(ZIO.succeed(List(1, 2)))
    val orderController = new OrderController(
      new TapirSecurity(ZLayer.succeed(authentication)),
      ZLayer.succeed(orderService)
    )

    // given
    val backendStub: SttpBackend[Future, Any] =
      TapirStubInterpreter(SttpBackendStub.asynchronousFuture)
        .whenServerEndpoint(orderController.createOrderEndpoint)
        .thenRunLogic()
        .backend()

    // when
    val response = basicRequest
      .post(uri"http://localhost:9000/orders")
      .body(
        CreateOrderForm(
          List(OrderProductForm(Util.generateUuid, 5)),
          "Some delivery comment"
        ).asJson.noSpaces
      )
      .header("Authorization", "Bearer password")
      .send(backendStub)

    // then
    response.map { resp =>
      logger.info(
        s"orders expected creating new order with Created http response"
      )
      resp.code shouldBe StatusCode.Created
    }
  }

  it should "Reject creating new user because some value about product is invalid" in {
    // preparations
    val orderService = mock[OrdersService]
    when(orderService.createOrder(any[String], any[CreateOrderForm]))
      .thenReturn(ZIO.succeed(List(1, 2)))
    val orderController = new OrderController(
      new TapirSecurity(ZLayer.succeed(authentication)),
      ZLayer.succeed(orderService)
    )

    // given
    val backendStub: SttpBackend[Future, Any] =
      TapirStubInterpreter(SttpBackendStub.asynchronousFuture)
        .whenServerEndpoint(orderController.createOrderEndpoint)
        .thenRunLogic()
        .backend()

    // when
    val response =
      basicRequest
        .post(uri"http://localhost:9000/orders")
        .body(
          CreateOrderForm(
            List(OrderProductForm("", 5)),
            "Some delivery comment"
          ).asJson.noSpaces
        )
        .header("Authorization", "Bearer password")
        .send(backendStub)

    // then
    response.map { resp =>
      logger.info(
        s"orders expected creating new order with Created http response"
      )
      resp.code shouldBe StatusCode.BadRequest
      resp.body shouldBe Left(
        BadRequest("Some order record contains invalid value!").asJson.noSpaces
      )
    }
  }

}

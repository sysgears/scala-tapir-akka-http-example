package com.example.controllers

import java.time.LocalDateTime
import com.example.auth.TapirSecurity
import com.example.errors.{BadRequest, NotFound}
import com.example.models.{Order, OrderRecord, OrderWithRecords, Product, Roles}
import com.example.models.forms.{CreateOrderForm, OrderProductForm}
import com.example.services.OrderService
import com.example.services.OrderService.OrdersService
import com.example.utils.{Util, ZioUtil}
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody
import io.circe.generic.auto._
import sttp.capabilities.WebSockets
import sttp.capabilities.akka.AkkaStreams
import sttp.tapir._
import sttp.model.StatusCode
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.{path, statusCode}
import zio.ULayer

import scala.concurrent.{ExecutionContext, Future}

/**
 * Contains endpoints, related to orders, in which user can access.
 *
 * @param tapirSecurity security endpoint.
 * @param orderService service for the controller.
 */
class OrderController(tapirSecurity: TapirSecurity, orderService: ULayer[OrdersService]) {

  /**
   * Create order endpoint.
   */
  val createOrderEndpoint = tapirSecurity.tapirSecurityEndpoint(List(Roles.User)) // accessible only for users with role User
    .post // POST endpoint
    .in("orders") // /orders uri
    .description("Creates order for the user")
    .in(jsonBody[CreateOrderForm] // request has to have body of CreateOrderForm
        .description("Contains everything for creating order")
        .example(CreateOrderForm(List(OrderProductForm(Util.generateUuid, 5)), "Some delivery comment")))
    .out(statusCode(StatusCode.Created).description("Returns Created when order is created")) // set static status code for success response
    .serverLogic { user => newOrder => // security output => endpoint input => server logic
      if (newOrder.products.forall(product => product.quantity > 0 && product.productId.nonEmpty)) {
        ZioUtil.foldRunToFuture(OrderService.createOrder(user.id, newOrder).provide(orderService))
      } else {
        Future.successful(Left(BadRequest("Some order record contains invalid value!")))
      }
    }

  /** get user's orders list view endpoint definition. */
  val viewUserOrderListEndpoint = tapirSecurity.tapirSecurityEndpoint(List(Roles.User)) // accessible only for users with role User
    .get // GET endpoint
    .description("Extracts orders for the user")
    .in("orders") // /orders uri
    .out(jsonBody[List[Order]] // defining response json format
      .description("List of orders for the user")
      .example(List(Order(Util.generateUuid, Util.generateUuid, LocalDateTime.now(), Order.NEW_STATUS, LocalDateTime.now(), "test comment"))))
    .serverLogic { user => _ => // endpoint logic definition
      ZioUtil.foldRunToFuture(OrderService.findOrdersForUser(user.id).provide(orderService))
    }

  /** get order details endpoint definition */
  val viewUserOrderEndpoint = tapirSecurity.tapirSecurityEndpoint(List(Roles.User)) // accessible only for users with role User
    .get // GET endpoint
    .description("Retrieves order details.")
    .in("orders" / path[String]("orderId").description("Order's id to retrieve information")) // /orders/:orderId uri
    .out(jsonBody[OrderWithRecords].description("Contains order itself with it's entries") // set response json format
      .example(OrderWithRecords(Order(Util.generateUuid, Util.generateUuid, LocalDateTime.now(), Order.NEW_STATUS, LocalDateTime.now(), "test comment"),
        List(OrderRecord(Some(Product(Util.generateUuid, "test product", "test description", 0.0)), 5)))))
    .serverLogic { _ => orderId => // endpoint logic definition.
      ZioUtil.foldRunToFuture(OrderService.getOrderDetails(orderId).provide(orderService))
    }

  /** Convenient way to assemble endpoints from the controller and then concat this route to main route. */
  val orderRoutes: List[ServerEndpoint[AkkaStreams with WebSockets, Future]] = List(createOrderEndpoint, viewUserOrderListEndpoint, viewUserOrderEndpoint)
}


package com.example.controllers

import java.time.LocalDateTime

import com.example.auth.TapirSecurity
import com.example.errors.BadRequest
import com.example.models.{Order, OrderRecord, OrderWithRecords, Product, Roles}
import com.example.models.forms.{CreateOrderForm, OrderProductForm}
import com.example.services.OrderService
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody
import io.circe.generic.auto._
import sttp.tapir._
import sttp.model.StatusCode
import sttp.tapir.{path, statusCode}

import scala.concurrent.{ExecutionContext, Future}

/**
 * Contains endpoints, related to orders, in which user can access.
 *
 * @param tapirSecurity security endpoint.
 * @param orderService service for the controller.
 * @param ec for futures.
 */
class OrderController(tapirSecurity: TapirSecurity, orderService: OrderService)(implicit ec: ExecutionContext) {

  /**
   * Create order endpoint.
   */
  val createOrderEndpoint = tapirSecurity.tapirSecurityEndpoint(List(Roles.User)) // accessible only for users with role User
    .post // POST endpoint
    .in("orders") // /orders uri
    .description("Creates order for the user")
    .in(jsonBody[CreateOrderForm] // request has to have body of CreateOrderForm
        .description("Contains everything for creating order")
        .example(CreateOrderForm(List(OrderProductForm(1, 5)), "Some delivery comment")))
    .out(statusCode(StatusCode.Created).description("Returns Created when order is created")) // set static status code for success response
    .serverLogic { user => newOrder => // security output => endpoint input => server logic
      if (newOrder.products.forall(product => product.quantity > 0 && product.productId > 0)) {
        orderService.createOrder(user.id, newOrder).map(_ => Right(()))
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
      .example(List(Order(0, 1, LocalDateTime.now(), Order.NEW_STATUS, LocalDateTime.now(), "test comment"))))
    .serverLogic { user => _ => // endpoint logic definition
      orderService.findOrdersForUser(user.id).map(Right(_))
    }

  /** get order details endpoint definition */
  val viewUserOrderEndpoint = tapirSecurity.tapirSecurityEndpoint(List(Roles.User)) // accessible only for users with role User
    .get // GET endpoint
    .description("Retrieves order details.")
    .in("orders" / path[Long]("orderId").description("Order's id to retrieve information")) // /orders/:orderId uri
    .out(jsonBody[OrderWithRecords].description("Contains order itself with it's entries") // set response json format
      .example(OrderWithRecords(Order(0, 1, LocalDateTime.now(), Order.NEW_STATUS, LocalDateTime.now(), "test comment"),
        List(OrderRecord(0, 0, Some(Product(0, "test product", "test description", 0.0)), 5)))))
    .serverLogic { _ => orderId => // endpoint logic definition.
      orderService.getOrderDetails(orderId).map {
        case Some(orderWithRecords) => Right(orderWithRecords)
        case None => Left(BadRequest("Order with that id not found"))
      }
    }

  /** Convenient way to assemble endpoints from the controller and then concat this route to main route. */
  val orderRoutes = List(createOrderEndpoint, viewUserOrderListEndpoint, viewUserOrderEndpoint)
}


package com.example.controllers

import java.time.LocalDateTime

import akka.http.scaladsl.server.{Directives, Route}
import com.example.auth.TapirSecurity
import com.example.dao.{OrderDao, OrderProductDao, ProductDao}
import com.example.models.{ErrorMessage, Order, OrderProduct, OrderRecord, Product, Roles, OrderWithRecords}
import com.example.models.forms.{CreateOrderForm, OrderProductForm}
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody
import io.circe.generic.auto._
import sttp.tapir._
import sttp.model.StatusCode
import sttp.tapir.server.akkahttp.AkkaHttpServerInterpreter
import sttp.tapir.{path, statusCode}

import scala.concurrent.{ExecutionContext, Future}

/**
 * Contains endpoints, related to orders, in which user can access.
 *
 * @param tapirSecurity security endpoint.
 * @param orderDao dao for orders.
 * @param productDao dao for products.
 * @param orderProductDao dao for orderProducts.
 * @param ec for futures.
 */
class OrderController(tapirSecurity: TapirSecurity,
                      orderDao: OrderDao,
                      productDao: ProductDao,
                      orderProductDao: OrderProductDao)(implicit ec: ExecutionContext) {

  /**
   * Create order endpoint.
   */
  val createOrderEndpoint: Route = AkkaHttpServerInterpreter().toRoute(tapirSecurity.tapirSecurityEndpoint(List(Roles.User)) // accessible only for users with role User
    .post // POST endpoint
    .in("orders") // /orders uri
    .in(jsonBody[CreateOrderForm] // request has to have body of CreateOrderForm
        .description("Contains everything for creating orders")
        .example(CreateOrderForm(List(OrderProductForm(1, 5)), "Some delivery comment")))
    .out(statusCode(StatusCode.Created).description("Returns Created when order is created")) // set static status code for success response
    .serverLogic { user => newOrder => // security output => endpoint input => server logic
      if (newOrder.products.forall(product => product.quantity > 0 && product.productId > 0)) {
        val order = Order(0, user.id, LocalDateTime.now(), Order.NEW_STATUS, LocalDateTime.now(), newOrder.comment)
        val products = newOrder.products.map(product => OrderProduct(0, 0, product.productId, product.quantity))
        for {
          orderId <- orderDao.insert(order)
          updatedProducts = products.map(_.copy(orderId = orderId))
          insertResult <- orderProductDao.insertBatch(updatedProducts)
        } yield {
          Right()
        }
      } else {
        Future.successful(Left(StatusCode.BadRequest, ErrorMessage("Some order record contains invalid value!")))
      }
    })

  /** get user's orders list view endpoint definition. */
  val viewUserOrderListEndpoint: Route = AkkaHttpServerInterpreter().toRoute(tapirSecurity.tapirSecurityEndpoint(List(Roles.User)) // accessible only for users with role User
    .get // GET endpoint
    .in("orders") // /orders uri
    .out(jsonBody[List[Order]] // defining response json format
      .description("Returns list of orders for the user")
      .example(List(Order(0, 1, LocalDateTime.now(), Order.NEW_STATUS, LocalDateTime.now(), "test comment"))))
    .serverLogic { user => _ => // endpoint logic definition
      orderDao.findForUser(user.id).map(Right(_))
    }
  )

  /** get order details endpoint definition */
  val viewUserOrderEndpoint: Route = AkkaHttpServerInterpreter().toRoute(tapirSecurity.tapirSecurityEndpoint(List(Roles.User)) // accessible only for users with role User
    .get // GET endpoint
    .in("orders" / path[Long]("orderId").description("Order's id to retrieve information")) // /orders/:orderId uri
    .out(jsonBody[OrderWithRecords].description("Contains order itself with it's entries") // set response json format
      .example(OrderWithRecords(Order(0, 1, LocalDateTime.now(), Order.NEW_STATUS, LocalDateTime.now(), "test comment"),
        List(OrderRecord(0, 0, Some(Product(0, "test product", "test description", 0.0)), 5)))))
    .serverLogic { _ => orderId => // endpoint logic definition.
      orderDao.find(orderId).flatMap {
        case Some(order) =>
          for {
            orderProducts <- orderProductDao.findByOrder(order.id)
            products <- productDao.findByIds(orderProducts.map(_.productId).distinct)
          } yield {
            val extendedOrderProducts = orderProducts.map { orderProduct =>
              val product = products.find(_.id == orderProduct.productId)
              OrderRecord(orderProduct.id, orderProduct.orderId, product, orderProduct.quantity)
            }
            Right(OrderWithRecords(order, extendedOrderProducts))
          }
        case None => Future.successful(Left(StatusCode.NotFound, ErrorMessage("Order with that id not found")))
      }
    }
  )

  /** Convenient way to assemble endpoints from the controller and then concat this route to main route. */
  val orderRoutes: Route = Directives.concat(createOrderEndpoint, viewUserOrderListEndpoint, viewUserOrderEndpoint)
}


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

class OrderController(tapirSecurity: TapirSecurity,
                      orderDao: OrderDao,
                      productDao: ProductDao,
                      orderProductDao: OrderProductDao)(implicit ec: ExecutionContext) {

  val createOrderEndpoint: Route = AkkaHttpServerInterpreter().toRoute(tapirSecurity.tapirSecurityEndpoint(List(Roles.User))
    .post
    .in("orders")
    .in(jsonBody[CreateOrderForm]
        .description("Contains everything for creating orders")
        .example(CreateOrderForm(List(OrderProductForm(1, 5)), "Some delivery comment")))
    .out(statusCode(StatusCode.Created).description("Returns Created when order is created"))
    .serverLogic { user => newOrder =>
      if (newOrder.products.forall(product => product.quantity > 0 && product.productId > 0)) {
        val order = Order(0, user.id, LocalDateTime.now(), "NEW", LocalDateTime.now(), newOrder.comment)
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

  val viewUserOrderListEndpoint: Route = AkkaHttpServerInterpreter().toRoute(tapirSecurity.tapirSecurityEndpoint(List(Roles.User))
    .get
    .in("orders")
    .out(jsonBody[List[Order]]
      .description("Returns list of orders for the user")
      .example(List(Order(0, 1, LocalDateTime.now(), "NEW", LocalDateTime.now(), "test comment"))))
    .serverLogic { user => _ =>
      orderDao.findForUser(user.id).map(Right(_))
    }
  )

  val viewUserOrderEndpoint: Route = AkkaHttpServerInterpreter().toRoute(tapirSecurity.tapirSecurityEndpoint(List(Roles.User))
    .get
    .in("orders" / path[Long]("orderId").description("Order's id to retrieve information"))
    .out(jsonBody[OrderWithRecords].description("Contains order itself with it's entries")
      .example(OrderWithRecords(Order(0, 1, LocalDateTime.now(), Order.NEW_STATUS, LocalDateTime.now(), "test comment"),
        List(OrderRecord(0, 0, Some(Product(0, "test product", "test description", 0.0)), 5)))))
    .serverLogic { _ => orderId =>
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

  val orderRoutes: Route = Directives.concat(createOrderEndpoint, viewUserOrderListEndpoint, viewUserOrderEndpoint)
}


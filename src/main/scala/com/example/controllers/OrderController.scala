package com.example.controllers

import java.time.LocalDateTime

import akka.http.scaladsl.server.{Directives, Route}
import com.example.auth.TapirSecurity
import com.example.dao.{OrderDao, OrderProductDao}
import com.example.models.{Order, OrderProduct, Roles}
import com.example.models.forms.{CreateOrderForm, OrderProductForm}
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody
import io.circe.generic.auto._
import sttp.model.StatusCode
import sttp.tapir.server.akkahttp.AkkaHttpServerInterpreter
import sttp.tapir.statusCode

import scala.concurrent.ExecutionContext

class OrderController(tapirSecurity: TapirSecurity,
                      orderDao: OrderDao,
                      orderProductDao: OrderProductDao)(implicit ec: ExecutionContext) {

  val createOrderEndpoint: Route = AkkaHttpServerInterpreter().toRoute(tapirSecurity.tapirSecurityEndpoint(List(Roles.User))
    .post
    .in("order")
    .in(jsonBody[CreateOrderForm]
        .description("Contains everything for creating orders")
        .example(CreateOrderForm(List(OrderProductForm(1, 5)), "Some delivery comment")))
    .out(statusCode(StatusCode.Created).description("Returns Created when order is created"))
    .serverLogic { user => newOrder =>
      val order = Order(0, user.id, LocalDateTime.now(), "NEW", LocalDateTime.now(), newOrder.comment)
      val products = newOrder.products.map(product => OrderProduct(0, 0, product.productId, product.quantity))
      for {
        orderId <- orderDao.insert(order)
        updatedProducts = products.map(_.copy(orderId = orderId))
        insertResult <- orderProductDao.insertBatch(updatedProducts)
      } yield {
        Right()
      }
    })

  val orderRoutes = Directives.concat(createOrderEndpoint)
}


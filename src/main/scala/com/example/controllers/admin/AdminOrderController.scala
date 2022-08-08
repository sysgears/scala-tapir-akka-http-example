package com.example.controllers.admin

import akka.http.scaladsl.server.{Directives, Route}
import com.example.auth.TapirSecurity
import com.example.dao.{OrderDao, OrderProductDao, ProductDao, UserDao}
import com.example.models.{AdminOrderViewResponse, AuthError, Order, OrderRecord, OrderWithRecords, PaginationMetadata, Roles, ShortUser, UserOrder}
import com.example.models.forms.{AdminOrderStatusChangeArguments, AdminViewPageArguments}
import sttp.tapir.server.akkahttp.AkkaHttpServerInterpreter
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody
import io.circe.generic.auto._
import sttp.model.StatusCode
import sttp.tapir._

import scala.concurrent.{ExecutionContext, Future}

class AdminOrderController(tapirSecurity: TapirSecurity,
                           orderDao: OrderDao,
                           productDao: ProductDao,
                           userDao: UserDao,
                           orderProductDao: OrderProductDao)(implicit ec: ExecutionContext) {

  val adminOrdersView = AkkaHttpServerInterpreter().toRoute(tapirSecurity.tapirSecurityEndpoint(List(Roles.Admin))
    .get
    .description("Showing paginated orders for admin with opportunity to sort by some parameters")
    .in("admin" / "orders")
    .in(EndpointInput.derived[AdminViewPageArguments])
    .out(jsonBody[AdminOrderViewResponse].description("Paginated list of orders, zipped with user, who made this order"))
    .serverLogic { _ => args =>
      if (args.page < 1 || args.pageSize < 1) {
        Future.successful(Left((StatusCode.BadRequest, AuthError("Page arguments are invalid!"))))
      } else {
        val offset = (args.page - 1) * args.pageSize
        val findPaginatedFuture = orderDao.findPaginated(args.pageSize, offset)
        val countOrdersFuture = orderDao.countOrders()
        for {
          orders <- findPaginatedFuture
          orderCount <- countOrdersFuture
          orderProducts <- orderProductDao.findByOrders(orders.map(_.id))
          products <- productDao.findByIds(orderProducts.map(_.productId).distinct)
          users <- userDao.findByIds(orders.map(_.userId).distinct)
        } yield {
          val userOrders = orders.map { order =>
            val extendedOrderProducts = orderProducts.filter(_.orderId == order.id).map { orderProduct =>
              val product = products.find(_.id == orderProduct.productId)
              OrderRecord(orderProduct.id, orderProduct.orderId, product, orderProduct.quantity)
            }
            val user = users.find(_.id == order.userId)
            UserOrder(user.map(ShortUser(_)), OrderWithRecords(order, extendedOrderProducts))
          }
          val pages = (orderCount.toDouble / args.pageSize.toDouble).ceil.toInt
          val metadata = PaginationMetadata(args.page, args.pageSize, pages, orderCount)
          Right(AdminOrderViewResponse(metadata, userOrders))
        }
      }
    }
  )

  val changeOrderStatusEndpoint = AkkaHttpServerInterpreter().toRoute(tapirSecurity.tapirSecurityEndpoint(List(Roles.Admin))
    .put
    .description("Updates order's status. Can change to unprocessed, processed or complete")
    .in(EndpointInput.derived[AdminOrderStatusChangeArguments])
    .out(jsonBody[String].description("Success message"))
    .serverLogic { _ => args =>
      if (Order.appropriateStatuses.contains(args.newStatus.toLowerCase())) {
        orderDao.updateStatus(args.orderId, args.newStatus.toLowerCase()).map(_ => Right("Updated!"))
      } else {
        Future.successful(Left((StatusCode.BadRequest, AuthError("Invalid new status!"))))
      }
    }
  )

  val deleteOrderEndpoint = AkkaHttpServerInterpreter().toRoute(tapirSecurity.tapirSecurityEndpoint(List(Roles.Admin))
    .delete
    .description("Removes order")
    .in("admin" / "orders").in(path[Long]("orderId"))
    .out(statusCode(StatusCode.NoContent))
    .serverLogic { _ => orderId =>
      orderDao.remove(orderId).map {
        case 0 => Left((StatusCode.NotFound, AuthError(s"Order $orderId not found")))
        case x if x > 0 => Right(())
        case _ => Left((StatusCode.InternalServerError, AuthError("Unknown error, got less 0 result")))
      }
    }
  )

  val adminOrderEndpoints: Route = Directives.concat(adminOrdersView, changeOrderStatusEndpoint, deleteOrderEndpoint)
}

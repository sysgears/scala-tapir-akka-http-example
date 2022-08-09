package com.example.controllers.admin

import akka.http.scaladsl.server.{Directives, Route}
import com.example.auth.TapirSecurity
import com.example.dao.{OrderDao, OrderProductDao, ProductDao, UserDao}
import com.example.models.{AdminOrderViewResponse, ErrorMessage, Order, OrderRecord, OrderWithRecords, PaginationMetadata, Roles, ShortUser, UserOrder}
import com.example.models.forms.{AdminOrderStatusChangeArguments, PaginatedEndpointArguments}
import sttp.tapir.server.akkahttp.AkkaHttpServerInterpreter
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody
import io.circe.generic.auto._
import sttp.model.StatusCode
import sttp.tapir._

import scala.concurrent.{ExecutionContext, Future}

/**
 * Contains admin endpoints, related to orders
 * @param tapirSecurity security endpoint
 * @param orderDao dao for orders
 * @param productDao dao for products
 * @param userDao dao for users
 * @param orderProductDao dao for order-product relations
 * @param ec for futures.
 */
class AdminOrderController(tapirSecurity: TapirSecurity,
                           orderDao: OrderDao,
                           productDao: ProductDao,
                           userDao: UserDao,
                           orderProductDao: OrderProductDao)(implicit ec: ExecutionContext) {

  /**
   * Retrieves paginated orders
   */
  val adminOrdersView = AkkaHttpServerInterpreter().toRoute(tapirSecurity.tapirSecurityEndpoint(List(Roles.Admin)) // restricted, admins only
    .get // GET endpoint
    .description("Showing paginated orders for admin with opportunity to sort by some parameters") // endpoint description
    .in("admin" / "orders") // /admin/orders uri
    .in(EndpointInput.derived[PaginatedEndpointArguments]) // args defined in that class
    .out(jsonBody[AdminOrderViewResponse].description("Paginated list of orders, zipped with user, who made this order")) // defined response
    .serverLogic { _ => args => // server logic
      if (args.page < 1 || args.pageSize < 1) { // page arguments validation, we don't want negative offset or page size
        Future.successful(Left((StatusCode.BadRequest, ErrorMessage("Page arguments are invalid!"))))
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
          val pages = (orderCount.toDouble / args.pageSize.toDouble).ceil.toInt // calculating amount of available pages
          val metadata = PaginationMetadata(args.page, args.pageSize, pages, orderCount)
          Right(AdminOrderViewResponse(metadata, userOrders))
        }
      }
    }
  )

  /** Changes order status */
  val changeOrderStatusEndpoint = AkkaHttpServerInterpreter().toRoute(tapirSecurity.tapirSecurityEndpoint(List(Roles.Admin))
    .put // PUT endpoint
    .description("Updates order's status. Can change to unprocessed, processed or complete") // endpoint description
    .in(EndpointInput.derived[AdminOrderStatusChangeArguments]) // defined arguments
    .out(jsonBody[String].description("Success message")) // defined response
    .serverLogic { _ => args => // server logic
      if (Order.appropriateStatuses.contains(args.newStatus.toLowerCase())) { // new status validation
        orderDao.updateStatus(args.orderId, args.newStatus.toLowerCase()).map(_ => Right("Updated!"))
      } else {
        Future.successful(Left((StatusCode.BadRequest, ErrorMessage("Invalid new status!"))))
      }
    }
  )

  /** Removes order from orders records */
  val deleteOrderEndpoint = AkkaHttpServerInterpreter().toRoute(tapirSecurity.tapirSecurityEndpoint(List(Roles.Admin))
    .delete // DELETE endpoint
    .description("Removes order")
    .in("admin" / "orders" / path[Long]("orderId").description("Id of order to delete").example(2)) // /admin/orders/:orderId uri
    .out(statusCode(StatusCode.NoContent)) // set static NoContent 204 status code on success.
    .serverLogic { _ => orderId => // server logic
      orderDao.remove(orderId).map {
        case 0 => Left((StatusCode.NotFound, ErrorMessage(s"Order $orderId not found"))) // if record wasn't removed
        case x if x > 0 => Right(()) // success
        case _ => Left((StatusCode.InternalServerError, ErrorMessage("Unknown error, got less 0 result"))) // unexpected result
      }
    }
  )

  /** Convenient way to assemble endpoints from the controller and then concat this route to main route. */
  val adminOrderEndpoints: Route = Directives.concat(adminOrdersView, changeOrderStatusEndpoint, deleteOrderEndpoint)
}

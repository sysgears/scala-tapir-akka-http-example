package com.example.controllers.admin

import akka.http.scaladsl.server.{Directives, Route}
import com.example.auth.TapirSecurity
import com.example.models.{AdminOrderViewResponse, ErrorMessage, Order, Roles}
import com.example.models.forms.{AdminOrderStatusChangeArguments, PaginatedEndpointArguments}
import com.example.services.admin.AdminOrderService
import sttp.tapir.server.akkahttp.AkkaHttpServerInterpreter
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody
import io.circe.generic.auto._
import sttp.model.StatusCode
import sttp.tapir._

import scala.concurrent.{ExecutionContext, Future}

/**
 * Contains admin endpoints, related to orders.
 *
 * @param tapirSecurity security endpoint
 * @param adminOrderService service for the controller.
 * @param ec for futures.
 */
class AdminOrderController(tapirSecurity: TapirSecurity, adminOrderService: AdminOrderService)(implicit ec: ExecutionContext) {

  /**
   * Retrieves paginated orders
   */
  val adminOrdersView: Route = AkkaHttpServerInterpreter().toRoute(tapirSecurity.tapirSecurityEndpoint(List(Roles.Admin)) // restricted, admins only
    .get // GET endpoint
    .description("Showing paginated orders for admin with opportunity to sort by some parameters") // endpoint description
    .in("admin" / "orders") // /admin/orders uri
    .in(EndpointInput.derived[PaginatedEndpointArguments]) // args defined in that class
    .out(jsonBody[AdminOrderViewResponse].description("Paginated list of orders, zipped with user, who made this order")) // defined response
    .serverLogic { _ => args => // server logic
      if (args.page < 1 || args.pageSize < 1) { // page arguments validation, we don't want negative offset or page size
        Future.successful(Left((StatusCode.BadRequest, ErrorMessage("Page arguments are invalid!"))))
      } else {
        adminOrderService.extractPaginatedOrders(args).map(Right(_))
      }
    }
  )

  /** Changes order status */
  val changeOrderStatusEndpoint: Route = AkkaHttpServerInterpreter().toRoute(tapirSecurity.tapirSecurityEndpoint(List(Roles.Admin))
    .put // PUT endpoint
    .description("Updates order's status. Can change to unprocessed, processed or complete") // endpoint description
    .in(EndpointInput.derived[AdminOrderStatusChangeArguments]) // defined arguments
    .out(jsonBody[String].description("Success message")) // defined response
    .serverLogic { _ => args => // server logic
      if (Order.appropriateStatuses.contains(args.newStatus.toLowerCase())) { // new status validation
        adminOrderService.updateOrderStatus(args).map {
          case 0 => Left((StatusCode.NotFound, ErrorMessage(s"Order ${args.orderId} not found"))) // if record wasn't updated
          case x if x > 0 => Right("Updated!") // success
          case _ => Left((StatusCode.InternalServerError, ErrorMessage("Unknown error, got less 0 result"))) // unexpected result
        }
      } else {
        Future.successful(Left((StatusCode.BadRequest, ErrorMessage("Invalid new status!"))))
      }
    }
  )

  /** Removes order from orders records */
  val deleteOrderEndpoint: Route = AkkaHttpServerInterpreter().toRoute(tapirSecurity.tapirSecurityEndpoint(List(Roles.Admin))
    .delete // DELETE endpoint
    .description("Removes order")
    .in("admin" / "orders" / path[Long]("orderId").description("Id of order to delete").example(2)) // /admin/orders/:orderId uri
    .out(statusCode(StatusCode.NoContent)) // set static NoContent 204 status code on success.
    .serverLogic { _ => orderId => // server logic
      adminOrderService.deleteOrder(orderId).map {
        case 0 => Left((StatusCode.NotFound, ErrorMessage(s"Order $orderId not found"))) // if record wasn't removed
        case x if x > 0 => Right(()) // success
        case _ => Left((StatusCode.InternalServerError, ErrorMessage("Unknown error, got less 0 result"))) // unexpected result
      }
    }
  )

  /** Convenient way to assemble endpoints from the controller and then concat this route to main route. */
  val adminOrderEndpoints: Route = Directives.concat(adminOrdersView, changeOrderStatusEndpoint, deleteOrderEndpoint)
}

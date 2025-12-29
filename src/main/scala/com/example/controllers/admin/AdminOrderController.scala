package com.example.controllers.admin

import com.example.auth.TapirSecurity
import com.example.errors.{BadRequest, InternalServerError, NotFound}
import com.example.models.{AdminOrderViewResponse, Order, Roles}
import com.example.models.forms.{AdminOrderStatusChangeArguments, PaginatedEndpointArguments}
import com.example.services.admin.AdminOrderService
import com.example.services.admin.AdminOrderService.AdminOrders
import com.example.utils.{Util, ZioUtil}
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody
import io.circe.generic.auto._
import sttp.model.StatusCode
import sttp.tapir._
import zio.ULayer

import scala.concurrent.{ExecutionContext, Future}

/**
 * Contains admin endpoints, related to orders.
 *
 * @param tapirSecurity security endpoint
 * @param adminOrderService service for the controller.
 */
class AdminOrderController(tapirSecurity: TapirSecurity, adminOrderService: ULayer[AdminOrders]) {

  /**
   * Retrieves paginated orders
   */
  val adminOrdersView = tapirSecurity.tapirSecurityEndpoint(List(Roles.Admin)) // restricted, admins only
    .get // GET endpoint
    .description("Showing paginated orders for admin with opportunity to sort by some parameters") // endpoint description
    .in("admin" / "orders") // /admin/orders uri
    .in(EndpointInput.derived[PaginatedEndpointArguments]) // args defined in that class
    .out(jsonBody[AdminOrderViewResponse].description("Paginated list of orders, zipped with user, who made this order")) // defined response
    .serverLogic { _ => args => // server logic
      if (args.page < 1 || args.pageSize < 1) { // page arguments validation, we don't want negative offset or page size
        Future.successful(Left(BadRequest("Page arguments are invalid!")))
      } else {
        ZioUtil.foldRunToFuture(AdminOrderService.extractPaginatedOrders(args).provide(adminOrderService))
      }
    }

  /** Changes order status */
  val changeOrderStatusEndpoint = tapirSecurity.tapirSecurityEndpoint(List(Roles.Admin))
    .put // PUT endpoint
    .description("Updates order's status. Can change to unprocessed, processed or complete") // endpoint description
    .in(EndpointInput.derived[AdminOrderStatusChangeArguments]) // defined arguments
    .out(jsonBody[String].description("Success message")) // defined response
    .serverLogic { _ => args => // server logic
      if (Order.appropriateStatuses.contains(args.newStatus.toLowerCase())) { // new status validation
        ZioUtil.foldRunToFuture(AdminOrderService.updateOrderStatus(args).provide(adminOrderService))
      } else {
        Future.successful(Left(BadRequest("Invalid new status!")))
      }
    }

  /** Removes order from orders records */
  val deleteOrderEndpoint = tapirSecurity.tapirSecurityEndpoint(List(Roles.Admin))
    .delete // DELETE endpoint
    .description("Removes order")
    .in("admin" / "orders" / path[String]("orderId").description("Id of order to delete").example(Util.generateUuid)) // /admin/orders/:orderId uri
    .out(statusCode(StatusCode.NoContent)) // set static NoContent 204 status code on success.
    .serverLogic { _ => orderId => // server logic
      ZioUtil.foldRunToFuture(AdminOrderService.deleteOrder(orderId).provide(adminOrderService))
    }

  /** Convenient way to assemble endpoints from the controller and then concat this route to main route. */
  val adminOrderEndpoints = List(adminOrdersView, changeOrderStatusEndpoint, deleteOrderEndpoint)
}

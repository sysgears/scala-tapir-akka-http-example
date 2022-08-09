package com.example.controllers

import akka.http.scaladsl.server.{Directives, Route}
import com.example.auth.TapirSecurity
import com.example.models.{ErrorMessage, PaginatedProductListViewResponse, Roles}
import com.example.models.forms.PaginatedEndpointArguments
import com.example.services.ProductService
import sttp.tapir.server.akkahttp.AkkaHttpServerInterpreter
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody
import io.circe.generic.auto._
import sttp.model.StatusCode
import sttp.tapir._

import scala.concurrent.{ExecutionContext, Future}

/**
 * Contains endpoints, related to products and available for user
 * @param tapirSecurity security endpoint
 * @param productService service for the controller.
 * @param ec for futures.
 */
class ProductController(tapirSecurity: TapirSecurity, productService: ProductService)(implicit ec: ExecutionContext) {

  /**
   * Retrieves paginated list of products.
   */
  val paginatedProductListEndpoint: Route = AkkaHttpServerInterpreter().toRoute(tapirSecurity.tapirSecurityEndpoint(List(Roles.User)) // restricted, only for users
    .get // GET endpoint
    .description("Shows paginated list of products for user") // endpoint description
    .in("products") // /products uri
    .in(EndpointInput.derived[PaginatedEndpointArguments]) // arguments described in that class
    .out(jsonBody[PaginatedProductListViewResponse].description("Contains pagination metadata and retrieved product list")) // defined response format
    .serverLogic { _ => args => // server logic
      if (args.page < 1 || args.pageSize < 1) { // page arguments validation, we don't want negative offset or page size
        Future.successful(Left((StatusCode.BadRequest, ErrorMessage("Page arguments are invalid!"))))
      } else {
        productService.extractPaginatedProducts(args).map(Right(_))
      }
    }
  )

  /** Convenient way to assemble endpoints from the controller and then concat this route to main route. */
  val productEndpoints: Route = Directives.concat(paginatedProductListEndpoint)
}

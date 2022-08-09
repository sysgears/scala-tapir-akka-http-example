package com.example.controllers

import akka.http.scaladsl.server.{Directives, Route}
import com.example.auth.TapirSecurity
import com.example.dao.ProductDao
import com.example.models.{ErrorMessage, PaginatedProductListViewResponse, PaginationMetadata, Roles}
import com.example.models.forms.PaginatedEndpointArguments
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
 * @param productDao dao for products
 * @param ec for futures.
 */
class ProductController(tapirSecurity: TapirSecurity,
                        productDao: ProductDao)(implicit ec: ExecutionContext) {

  /**
   * Retrieves paginated list of products.
   */
  val paginatedProductListEndpoint = AkkaHttpServerInterpreter().toRoute(tapirSecurity.tapirSecurityEndpoint(List(Roles.User)) // restricted, only for users
    .get // GET endpoint
    .description("Shows paginated list of products for user") // endpoint description
    .in("products") // /products uri
    .in(EndpointInput.derived[PaginatedEndpointArguments]) // arguments described in that class
    .out(jsonBody[PaginatedProductListViewResponse].description("Contains pagination metadata and retrieved product list")) // defined response format
    .serverLogic { _ => args => // server logic
      if (args.page < 1 || args.pageSize < 1) { // page arguments validation, we don't want negative offset or page size
        Future.successful(Left((StatusCode.BadRequest, ErrorMessage("Page arguments are invalid!"))))
      } else {
        val offset = (args.page - 1) * args.pageSize
        val findPaginatedFuture = productDao.findPaginated(args.pageSize, offset)
        val countProductsFuture = productDao.countProducts()
        for {
          products <- findPaginatedFuture
          productsCount <- countProductsFuture
        } yield {
          val pages = (productsCount.toDouble / args.pageSize.toDouble).ceil.toInt
          val metadata = PaginationMetadata(args.page, args.pageSize, pages, productsCount)
          Right(PaginatedProductListViewResponse(metadata, products))
        }
      }
    }
  )

  /** Convenient way to assemble endpoints from the controller and then concat this route to main route. */
  val productEndpoints: Route = Directives.concat(paginatedProductListEndpoint)
}

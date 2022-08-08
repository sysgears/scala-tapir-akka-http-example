package com.example.controllers

import akka.http.scaladsl.server.{Directives, Route}
import com.example.auth.TapirSecurity
import com.example.dao.ProductDao
import com.example.models.{AuthError, PaginatedProductListViewResponse, PaginationMetadata, Roles}
import com.example.models.forms.PaginatedEndpointArguments
import sttp.tapir.server.akkahttp.AkkaHttpServerInterpreter
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody
import io.circe.generic.auto._
import sttp.model.StatusCode
import sttp.tapir._

import scala.concurrent.{ExecutionContext, Future}

class ProductController(tapirSecurity: TapirSecurity,
                        productDao: ProductDao)(implicit ec: ExecutionContext) {

  val paginatedProductListEndpoint = AkkaHttpServerInterpreter().toRoute(tapirSecurity.tapirSecurityEndpoint(List(Roles.User))
    .get
    .description("Shows paginated list of products for user")
    .in("products")
    .in(EndpointInput.derived[PaginatedEndpointArguments])
    .out(jsonBody[PaginatedProductListViewResponse].description("Contains pagination metadata and "))
    .serverLogic { _ => args =>
      if (args.page < 1 || args.pageSize < 1) {
        Future.successful(Left((StatusCode.BadRequest, AuthError("Page arguments are invalid!"))))
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

  val productEndpoints: Route = Directives.concat(paginatedProductListEndpoint)
}

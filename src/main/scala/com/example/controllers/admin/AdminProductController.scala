package com.example.controllers.admin

import com.example.auth.TapirSecurity
import com.example.errors.{InternalServerError, NotFound}
import com.example.models.forms.NewProductForm
import com.example.models.{Product, Roles}
import com.example.services.admin.AdminProductService
import com.example.services.admin.AdminProductService.AdminProducts
import com.example.utils.{Util, ZioUtil}
import com.typesafe.scalalogging.LazyLogging
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody
import io.circe.generic.auto._
import sttp.capabilities.WebSockets
import sttp.capabilities.akka.AkkaStreams
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.server.ServerEndpoint
import zio.ULayer

import scala.concurrent.{ExecutionContext, Future}

/**
  * Contains admin products endpoints.
  *
  * @param tapirSecurity       security endpoint.
  * @param adminProductService controller service.
  */
class AdminProductController(tapirSecurity: TapirSecurity,
                             adminProductService: ULayer[AdminProducts])
    extends LazyLogging {

  /**
    * Extracts all products.
    */
  val adminProductsViewEndpoint = tapirSecurity
    .tapirSecurityEndpoint(List(Roles.Admin))
    .get // GET endpoint
    .description("Extracts products list for the admin") // endpoint description
    .in("admin" / "products") // /admin/products uri
    .out(
      jsonBody[List[Product]]
        .description("List of products")
        .example(
          List(
            Product(Util.generateUuid, "test product", "test description", 5.0)
          )
        )
    ) // defined response
    .serverLogic { _ => _ => // endpoint logic
      ZioUtil.foldRunToFuture(
        AdminProductService.findAllProducts().provide(adminProductService)
    )
    }

  /**
    * Creates new product.
    */
  val createProductEndpoint = tapirSecurity
    .tapirSecurityEndpoint(List(Roles.Admin))
    .post // POST endpoint
    .description("Creates new product") // endpoint description
    .in("admin" / "products") // /admin/products uri
    .in(
      jsonBody[NewProductForm]
        .description("Entity with data to create new product")
        .example(NewProductForm("test product", "test description", 5.0))
    ) // defines request body
    .out(statusCode(StatusCode.Created)) // defined static success response http code.
    .serverLogic { _ => newProductForm => // endpoint logic
      ZioUtil.foldRunToFuture(
        AdminProductService.insert(newProductForm).provide(adminProductService)
    )
    }

  /**
    * Updates product.
    */
  val updateProductEndpoint = tapirSecurity
    .tapirSecurityEndpoint(List(Roles.Admin))
    .put // PUT endpoint
    .description("Updates existing product") // endpoint description
    .in(
      "admin" / "products" / path[String]("productId")
        .example(Util.generateUuid)
    ) // /admin/products/:productId
    .in(jsonBody[Product].description("Product with new updates")) // defined request body
    .out(jsonBody[String].description("Returns success message")) // defined response body
    .serverLogic { _ => args => // endpoint logic
      val product = args._2
      ZioUtil.foldRunToFuture(
        AdminProductService.update(product).provide(adminProductService)
    )
    }

  /**
    * Removes product.
    */
  val deleteProductEndpoint = tapirSecurity
    .tapirSecurityEndpoint(List(Roles.Admin))
    .delete // DELETE endpoint
    .description("Removes product from product list") // endpoint description
    .in(
      "admin" / "products" / path[String]("productId")
        .description("Id of product to delete")
        .example(Util.generateUuid)
    ) // /admin/products/:productId uri
    .out(
      statusCode(StatusCode.NoContent)
        .description("Returns no content for delete endpoint")
    ) // defined static 204 NoContent
    .serverLogic { _ => productId =>
      ZioUtil.foldRunToFuture(
        AdminProductService.remove(productId).provide(adminProductService)
      )
    }

  /** Convenient way to assemble endpoints from the controller and then concat this route to main route. */
  val adminProductEndpoints
    : List[ServerEndpoint[AkkaStreams with WebSockets, Future]] = List(
    adminProductsViewEndpoint,
    createProductEndpoint,
    updateProductEndpoint,
    deleteProductEndpoint
  )

}

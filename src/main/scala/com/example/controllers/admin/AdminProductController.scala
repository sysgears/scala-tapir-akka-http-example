package com.example.controllers.admin

import akka.http.scaladsl.server.{Directives, Route}
import com.example.auth.TapirSecurity
import com.example.errors.ErrorHandler
import com.example.models.forms.NewProductForm
import com.example.models.{ErrorMessage, Product, Roles}
import com.example.services.admin.AdminProductService
import sttp.tapir.server.akkahttp.AkkaHttpServerInterpreter
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody
import io.circe.generic.auto._
import sttp.model.StatusCode
import sttp.tapir._

import scala.concurrent.ExecutionContext

/**
 * Contains admin products endpoints.
 *
 * @param tapirSecurity security endpoint.
 * @param adminProductService controller service.
 * @param ec for futures.
 */
class AdminProductController(tapirSecurity: TapirSecurity, adminProductService: AdminProductService, errorHandler: ErrorHandler)(implicit ec: ExecutionContext) {

  /**
   * Extracts all products.
   */
  val adminProductsViewEndpoint: Route = AkkaHttpServerInterpreter(errorHandler.customServerOptions).toRoute(tapirSecurity.tapirSecurityEndpoint(List(Roles.Admin))
    .get // GET endpoint
    .description("Extracts products list for the admin") // endpoint description
    .in("admin" / "products") // /admin/products uri
    .out(jsonBody[List[Product]].description("List of products").example(List(Product(0, "test product", "test description", 5.0)))) // defined response
    .serverLogic { _ => _ => // endpoint logic
      adminProductService.findAllProducts().map(Right(_))
    }
  )

  /**
   * Creates new product.
   */
  val createProductEndpoint: Route = AkkaHttpServerInterpreter(errorHandler.customServerOptions).toRoute(tapirSecurity.tapirSecurityEndpoint(List(Roles.Admin))
    .post // POST endpoint
    .description("Creates new product") // endpoint description
    .in("admin" / "products") // /admin/products uri
    .in(jsonBody[NewProductForm].description("Entity with data to create new product")
      .example(NewProductForm("test product", "test description", 5.0))) // defines request body
    .out(statusCode(StatusCode.Created)) // defined static success response http code.
    .serverLogic { _ => newProductForm => // endpoint logic
      adminProductService.insert(newProductForm).map(_ => Right())
    }
  )

  /**
   * Updates product.
   */
  val updateProductEndpoint: Route = AkkaHttpServerInterpreter(errorHandler.customServerOptions).toRoute(tapirSecurity.tapirSecurityEndpoint(List(Roles.Admin))
    .put // PUT endpoint
    .description("Updates existing product") // endpoint description
    .in("admin" / "products" / path[Long]("productId").example(2)) // /admin/products/:productId
    .in(jsonBody[Product].description("Product with new updates")) // defined request body
    .out(jsonBody[String].description("Returns success message")) // defined response body
    .serverLogic { _ => args => // endpoint logic
      val product = args._2
      adminProductService.update(product).map {
        case 0 => Left((StatusCode.NotFound, ErrorMessage(s"Product ${product.id} not found"))) // if record wasn't removed
        case x if x > 0 => Right("Updated!") // success
        case _ => Left((StatusCode.InternalServerError, ErrorMessage("Unknown error, got less 0 result"))) // unexpected result
      }
    }
  )

  /**
   * Removes product.
   */
  val deleteProductEndpoint: Route = AkkaHttpServerInterpreter(errorHandler.customServerOptions).toRoute(tapirSecurity.tapirSecurityEndpoint(List(Roles.Admin))
    .delete // DELETE endpoint
    .description("Removes product from product list") // endpoint description
    .in("admin" / "products" / path[Long]("productId").description("Id of product to delete").example(2)) // /admin/products/:productId uri
    .out(statusCode(StatusCode.NoContent).description("Returns no content for delete endpoint")) // defined static 204 NoContent
    .serverLogic { _ => productId =>
      adminProductService.remove(productId).map {
        case 0 => Left((StatusCode.NotFound, ErrorMessage(s"Product $productId not found"))) // if record wasn't removed
        case x if x > 0 => Right(()) // success
        case _ => Left((StatusCode.InternalServerError, ErrorMessage("Unknown error, got less 0 result"))) // unexpected result
      }
    }
  )

  /** Convenient way to assemble endpoints from the controller and then concat this route to main route. */
  val adminProductEndpoints: Route = Directives.concat(adminProductsViewEndpoint, createProductEndpoint,
    updateProductEndpoint, deleteProductEndpoint)

}

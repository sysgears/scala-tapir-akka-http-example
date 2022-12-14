package com.example.controllers.admin

import com.example.auth.TapirSecurity
import com.example.errors.{InternalServerError, NotFound}
import com.example.models.forms.NewProductForm
import com.example.models.{Product, Roles}
import com.example.services.admin.AdminProductService
import com.typesafe.scalalogging.LazyLogging
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
class AdminProductController(tapirSecurity: TapirSecurity, adminProductService: AdminProductService)(implicit ec: ExecutionContext) extends LazyLogging {

  /**
   * Extracts all products.
   */
  val adminProductsViewEndpoint = tapirSecurity.tapirSecurityEndpoint(List(Roles.Admin))
    .get // GET endpoint
    .description("Extracts products list for the admin") // endpoint description
    .in("admin" / "products") // /admin/products uri
    .out(jsonBody[List[Product]].description("List of products").example(List(Product(0, "test product", "test description", 5.0)))) // defined response
    .serverLogic { _ => _ => // endpoint logic
      adminProductService.findAllProducts().map(Right(_))
    }

  /**
   * Creates new product.
   */
  val createProductEndpoint = tapirSecurity.tapirSecurityEndpoint(List(Roles.Admin))
    .post // POST endpoint
    .description("Creates new product") // endpoint description
    .in("admin" / "products") // /admin/products uri
    .in(jsonBody[NewProductForm].description("Entity with data to create new product")
      .example(NewProductForm("test product", "test description", 5.0))) // defines request body
    .out(statusCode(StatusCode.Created)) // defined static success response http code.
    .serverLogic { _ => newProductForm => // endpoint logic
      adminProductService.insert(newProductForm).map(_ => Right())
    }

  /**
   * Updates product.
   */
  val updateProductEndpoint = tapirSecurity.tapirSecurityEndpoint(List(Roles.Admin))
    .put // PUT endpoint
    .description("Updates existing product") // endpoint description
    .in("admin" / "products" / path[Long]("productId").example(2)) // /admin/products/:productId
    .in(jsonBody[Product].description("Product with new updates")) // defined request body
    .out(jsonBody[String].description("Returns success message")) // defined response body
    .serverLogic { _ => args => // endpoint logic
      val product = args._2
      adminProductService.update(product).map {
        case 0 => Left(NotFound(s"Product ${product.id} not found")) // if record wasn't removed
        case x if x > 0 => Right("Updated!") // success
        case _ =>
          logger.error(s"Intercepted unusual case when response from database is less than 0, PUT /admin/products/${product.id} endpoint, update product: $product")
          Left(InternalServerError("Unknown error, got less 0 result")) // unexpected result
      }
    }

  /**
   * Removes product.
   */
  val deleteProductEndpoint = tapirSecurity.tapirSecurityEndpoint(List(Roles.Admin))
    .delete // DELETE endpoint
    .description("Removes product from product list") // endpoint description
    .in("admin" / "products" / path[Long]("productId").description("Id of product to delete").example(2)) // /admin/products/:productId uri
    .out(statusCode(StatusCode.NoContent).description("Returns no content for delete endpoint")) // defined static 204 NoContent
    .serverLogic { _ => productId =>
      adminProductService.remove(productId).map {
        case 0 => Left(NotFound(s"Product $productId not found")) // if record wasn't removed
        case x if x > 0 => Right(()) // success
        case _ =>
          logger.error(s"Intercepted unusual case when response from database is less than 0, DELETE /admin/products/$productId endpoint, delete product with id: $productId")
          Left(InternalServerError("Unknown error, got less 0 result")) // unexpected result
      }
    }

  /** Convenient way to assemble endpoints from the controller and then concat this route to main route. */
  val adminProductEndpoints = List(adminProductsViewEndpoint, createProductEndpoint,
    updateProductEndpoint, deleteProductEndpoint)

}

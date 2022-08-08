package com.example.controllers.admin

import akka.http.scaladsl.server.{Directives, Route}
import com.example.auth.TapirSecurity
import com.example.dao.ProductDao
import com.example.models.forms.NewProductForm
import com.example.models.{Product, Roles}
import sttp.tapir.server.akkahttp.AkkaHttpServerInterpreter
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody
import io.circe.generic.auto._
import sttp.model.StatusCode
import sttp.tapir._

import scala.concurrent.ExecutionContext

class AdminProductController(tapirSecurity: TapirSecurity,
                             productDao: ProductDao)(implicit ec: ExecutionContext) {

  val adminProductsViewEndpoint: Route = AkkaHttpServerInterpreter().toRoute(tapirSecurity.tapirSecurityEndpoint(List(Roles.Admin))
    .get
    .description("Extracts products list for the admin")
    .in("admin" / "products")
    .out(jsonBody[List[Product]].description("List of products").example(List(Product(0, "test product", "test description", 5.0))))
    .serverLogic { _ => _ =>
      productDao.findAll().map(Right(_))
    }
  )

  val createProductEndpoint: Route = AkkaHttpServerInterpreter().toRoute(tapirSecurity.tapirSecurityEndpoint(List(Roles.Admin))
    .post
    .description("Creates new product")
    .in("admin" / "products")
    .in(jsonBody[NewProductForm].description("Entity with data to create new product").example(NewProductForm("test product", "test description", 5.0)))
    .out(statusCode(StatusCode.Created))
    .serverLogic { _ => newProductForm =>
      productDao.insert(Product(0, newProductForm.name, newProductForm.description, newProductForm.price)).map(_ => Right())
    }
  )

  val adminProductEndpoints: Route = Directives.concat(adminProductsViewEndpoint, createProductEndpoint)

}
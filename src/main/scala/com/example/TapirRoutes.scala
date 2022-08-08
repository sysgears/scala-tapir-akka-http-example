package com.example

import akka.http.scaladsl.Http
import akka.http.scaladsl.server.{Directives, Route}
import com.example.modules.MainModule
import com.typesafe.scalalogging.LazyLogging
import io.circe.syntax.EncoderOps
import sttp.model.StatusCode
import sttp.tapir.server.akkahttp.AkkaHttpServerInterpreter
import sttp.tapir._

import scala.concurrent.Future
import scala.io.StdIn

/**
 * Application's init point.
 *
 * Extends main module, which contains all wirings and starts the server. Also contains test route for first example.
 */
class TapirRoutes extends LazyLogging with MainModule {

  // mostly for execution context
  import actorSystem.dispatcher

  /*
    test tapir endpoint. This endpoint continues security endpoint.
  */
  val tapirEndpoint = tapirSecurity
    .tapirSecurityEndpoint(List.empty) // no rule restriction (authorization)
    .get // http type
    .description("test endpoint") // endpoint's description
    .in("test".description("endpoint path")) // description for uri path
    .out(stringBody.description("type of response")) // This endpoint will return string body. Also, description for body
    .out(statusCode(StatusCode.Created).description("Specifies response status code for success case")) // Description for result status code

  val route: Route = AkkaHttpServerInterpreter().toRoute(tapirEndpoint.serverLogic { user => _ =>
    // first argument from security, second from endpoint specification (described in 'in' functions)
    Future(Right(s"test ok response with user $user")) // response in Right for success, left for error
  })

  /**
   * Result route. Contains all active endpoints and this route will be bound to the server.
   */
  val resultRoute: Route = Directives.concat(route,
    authController.authRoutes,
    orderController.orderRoutes,
    productController.productEndpoints,
    adminProductController.adminProductEndpoints,
    adminOrderController.adminOrderEndpoints)

  /**
   * Starts server using route above.
   */
  def init(): Unit = {
    val bindingFuture = Http().newServerAt("localhost", 9000).bind(resultRoute)
    logger.info(s"Server online at http://localhost:9000/")
    logger.info("Press RETURN to stop...")
    StdIn.readLine()
    logger.info("Going offline....")
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => actorSystem.terminate())
  }
}

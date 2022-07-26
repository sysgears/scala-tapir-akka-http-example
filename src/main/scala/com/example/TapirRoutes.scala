package com.example

import akka.http.scaladsl.Http
import com.example.modules.MainModule
import com.typesafe.scalalogging.LazyLogging
import sttp.model.StatusCode
import sttp.tapir.server.akkahttp.AkkaHttpServerInterpreter
import sttp.tapir._

import scala.concurrent.Future
import scala.io.StdIn

object TapirRoutes extends App with LazyLogging with MainModule {

  import actorSystem.dispatcher

  val tapirEndpoint = tapirSecurity.tapirSecurityEndpoint(List.empty).get.in("test").out(stringBody).out(statusCode(StatusCode.Created))

  val route = AkkaHttpServerInterpreter().toRoute(tapirEndpoint.serverLogic { user => input =>
    Future(Right(s"test ok response with user $user"))
    /* here we can use both `special` and `input` values */
  })

  val bindingFuture = Http().newServerAt("localhost", 9000).bind(route)
  logger.info(s"Server online at http://localhost:9000/")
  logger.info("Press RETURN to stop...")
  StdIn.readLine()
  logger.info("Going offline....")
  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => actorSystem.terminate())
}

package com.example.utils

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Directives.{extractRequestContext, mapInnerRoute}
import akka.http.scaladsl.server.{Directive0, RouteResult}
import akka.http.scaladsl.server.RouteResult.{Complete, Rejected}
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

/**
 * Feature to track time for request handling.
 *
 * Implementation was taken and slightly modified from https://blog.softwaremill.com/measuring-response-time-in-akka-http-7b6312ec70cf
 *
 * @param ec for futures.
 */
class RequestTimeTracker(implicit ec: ExecutionContext) extends LazyLogging {

  /**
   * Starts timer and returns function, which will stop the timer and log time with some details.
   *
   * @param request request, on which which will be logged after handling request.
   * @return function, which will handle response for request.
   */
  def timeRequest(request: HttpRequest): Try[RouteResult] => Unit = {
    val start = System.currentTimeMillis()

    {
      case Success(Complete(resp)) =>
        val d = System.currentTimeMillis() - start
        logger.info(s"[${resp.status.intValue()}] ${request.method.name} " +
          s"${request.uri.path}, took: ${d}ms")
      case Success(Rejected(_)) =>
      case Failure(_) =>
    }
  }

  /**
   * Directive-wrapper for request.
   *
   * @param onRequest action, which accepts request and return another function, which accepts response.
   * @return ready directive, which can be used for wrapping another directives.
   */
  def aroundRequest(onRequest: HttpRequest => Try[RouteResult] => Unit): Directive0 =
    extractRequestContext.flatMap { ctx =>
      val onDone = onRequest(ctx.request) // starts timer for request and returns function, which you will use to stop timer and log request time
      mapInnerRoute { inner =>
        inner.andThen { resultFuture =>
          resultFuture.map {
            case c @ Complete(response) =>
              Complete(response.mapEntity { entity =>
                if (entity.isKnownEmpty()) { // stops timer now because response is empty
                  onDone(Success(c))
                  entity
                } else {
                  // On an empty entity, `transformDataBytes` unsets `isKnownEmpty`.
                  // Call onDone right away, since there's no significant amount of
                  // data to send, anyway.
                  entity.transformDataBytes(Flow[ByteString].watchTermination() {
                    case (mat, future) =>
                      future.map(_ => c).onComplete(onDone) // stops timer after finishing sending response
                      mat
                  })
                }
              })
            case other =>
              onDone(Success(other)) // stops timer and returns other
              other
          }.andThen { // skip this if you use akka.http.scaladsl.server.handleExceptions, put onDone there
            case Failure(ex) =>
              onDone(Failure(ex)) // stops timer and returns failure
          }
        }
      }
    }
}

package com.example.errors

import java.util.UUID

import com.typesafe.scalalogging.LazyLogging
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.generic.auto._
import sttp.tapir._
import sttp.tapir.server.akkahttp.AkkaHttpServerOptions
import sttp.tapir.server.interceptor.decodefailure.DefaultDecodeFailureHandler
import sttp.tapir.server.interceptor.decodefailure.DefaultDecodeFailureHandler.FailureMessages
import sttp.tapir.server.interceptor.exception.ExceptionHandler
import sttp.tapir.server.metrics.prometheus.PrometheusMetrics
import sttp.tapir.server.model.ValuedEndpointOutput
import io.circe.generic.auto._

import scala.concurrent.{ExecutionContext, Future}

/**
 * Contains error handler interceptors with additional interceptors
 * @param ec for futures
 */
class ErrorHandler(implicit ec: ExecutionContext) extends LazyLogging {

  /** Prometheus metrics interceptor. */
  val prometheusMetrics = PrometheusMetrics.default[Future]()

  /**
   * Configuration for AkkaHttpServer routes.
   *
   * Contains customization for decode failure handler, exception handler and applied metrics interceptor
   */
  implicit val customServerOptions: AkkaHttpServerOptions = AkkaHttpServerOptions.customiseInterceptors
    .decodeFailureHandler(ctx => {
      ctx.failingInput match {
        // when defining how a decode failure should be handled, we need to describe the output to be used, and
        // a value for this output
        case _: EndpointIO.Body[_, _] =>
          // see this function and then to failureSourceMessage function to find out which types of decode errors are present
          val failureMessage = FailureMessages.failureMessage(ctx)
          logger.info(s"$failureMessage")
          // warning - log working incorrect when there are several endpoints with different methods
          DefaultDecodeFailureHandler.default(ctx)
        case _ => DefaultDecodeFailureHandler.default(ctx)
      }
    })
    .exceptionHandler(ExceptionHandler[Future] { ctx =>
      val exceptionId = UUID.randomUUID() // defining exception id for the exception to make search in logs easier.
      logger.error(s"Intercepted exception ${ctx.e} while processing request, exception id: $exceptionId")
      Future.successful(Some(ValuedEndpointOutput[ErrorMessage](jsonBody[ErrorMessage], ErrorMessage(s"Internal Server Error, exception id: $exceptionId"))))
    })
    .metricsInterceptor(prometheusMetrics.metricsInterceptor())
    .options

}

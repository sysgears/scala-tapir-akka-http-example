package com.example.errors

import java.util.UUID

import com.example.models.ErrorMessage
import com.typesafe.scalalogging.LazyLogging
import sttp.model.StatusCode
import sttp.tapir.EndpointInput
import sttp.tapir.{server, _}
import sttp.tapir.server.akkahttp.AkkaHttpServerOptions
import sttp.tapir.server.interceptor.decodefailure.DefaultDecodeFailureHandler
import sttp.tapir.server.interceptor.decodefailure.DefaultDecodeFailureHandler.FailureMessages
import sttp.tapir.server.interceptor.exception.ExceptionHandler
import sttp.tapir.server.metrics.prometheus.PrometheusMetrics
import sttp.tapir.server.model.ValuedEndpointOutput

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}


class ErrorHandler(implicit ec: ExecutionContext) extends LazyLogging {
  val prometheusMetrics = PrometheusMetrics.default[Future]()

  implicit val customServerOptions: AkkaHttpServerOptions = AkkaHttpServerOptions.customiseInterceptors
    .decodeFailureHandler(ctx => {
      ctx.failingInput match {
        // when defining how a decode failure should be handled, we need to describe the output to be used, and
        // a value for this output
        case _: EndpointIO.Body[_, _] =>
          // see this function and then to failureSourceMessage function to find out which types of decode errors are present
          val failureMessage = FailureMessages.failureMessage(ctx)
          logger.info(s"${ctx.endpoint.showShort} - $failureMessage")
          // warning - log working incorrect when there are several endpoints for
          DefaultDecodeFailureHandler.default(ctx)
        case _ => DefaultDecodeFailureHandler.default(ctx)
      }
    })
    .metricsInterceptor(prometheusMetrics.metricsInterceptor())
    .options

}

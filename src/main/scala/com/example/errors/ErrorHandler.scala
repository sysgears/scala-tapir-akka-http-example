package com.example.errors

import java.util.UUID
import com.typesafe.scalalogging.LazyLogging
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.generic.auto._
import sttp.tapir._
import sttp.tapir.server.akkahttp.AkkaHttpServerOptions
import sttp.tapir.server.interceptor.decodefailure.{DecodeFailureHandler, DefaultDecodeFailureHandler}
import sttp.tapir.server.interceptor.decodefailure.DefaultDecodeFailureHandler.FailureMessages
import sttp.tapir.server.interceptor.exception.ExceptionHandler
import sttp.tapir.server.metrics.prometheus.PrometheusMetrics
import sttp.tapir.server.model.ValuedEndpointOutput
import io.circe.generic.auto._
import sttp.monad.MonadError
import sttp.tapir.server.interceptor.DecodeFailureContext

import scala.concurrent.{ExecutionContext, Future}

/** Contains error handler interceptors with additional interceptors
  *
  * @param ec for futures
  */
class ErrorHandler(implicit ec: ExecutionContext) extends LazyLogging {

  /** Prometheus metrics interceptor. */
  val prometheusMetrics = PrometheusMetrics.default[Future]()

  /** Configuration for AkkaHttpServer routes.
    *
    * Contains customization for decode failure handler, exception handler and applied metrics interceptor
    */
  implicit val customServerOptions: AkkaHttpServerOptions = AkkaHttpServerOptions.customiseInterceptors
    .decodeFailureHandler(new DecodeFailureHandler[Future] {

      override def apply(ctx: DecodeFailureContext)(implicit
          monad: MonadError[Future]
      ): Future[Option[ValuedEndpointOutput[_]]] = {
        ctx.failingInput match {
          case _: EndpointIO.Body[_, _] =>
            val failureMessage = FailureMessages.failureMessage(ctx)
            logger.info(s"$failureMessage")
          case _ => ()
        }
        // Delegate to default handler
        DefaultDecodeFailureHandler[Future](ctx)
      }
    })
    .exceptionHandler(ExceptionHandler[Future] { ctx =>
      val exceptionId = UUID.randomUUID() // defining exception id for the exception to make search in logs easier.
      logger.error(
        s"Intercepted exception ${ctx.e} while processing request, exception id: $exceptionId"
      )
      Future.successful(
        Some(
          ValuedEndpointOutput[ErrorMessage](
            jsonBody[ErrorMessage],
            ErrorMessage(s"Internal Server Error, exception id: $exceptionId")
          )
        )
      )
    })
    .metricsInterceptor(prometheusMetrics.metricsInterceptor())
    .options

}

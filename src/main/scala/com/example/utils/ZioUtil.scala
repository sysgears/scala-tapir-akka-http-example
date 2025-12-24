package com.example.utils

import com.example.errors.{ErrorInfo, InternalServerError}
import com.example.services.admin.AdminOrderService.logger
import com.typesafe.scalalogging.LazyLogging
import zio.{UIO, Unsafe, ZIO}

import java.sql.SQLException
import scala.concurrent.Future

object ZioUtil extends LazyLogging {


  /**
   * Exiting zio execution. Use only on edges of zio integration.
   * @param monad
   * @tparam T monad result
   * @return
   */
  def runToFuture[T](monad: UIO[T]): Future[T] = {
    Unsafe.unsafe { implicit unsafe =>
      zio.Runtime.default.unsafe.runToFuture(monad).future
    }
  }

  /**
   * Same as runToFuture, but also places both failed and successful results into Either
   * @param monad
   * @tparam E error type
   * @tparam T success type
   * @return Future with Either, which contains both failure and success
   */
  def foldRunToFuture[E, T](monad: ZIO[Any, E, T]): Future[Either[E, T]] = {
    ZioUtil.runToFuture(monad.fold(error => Left(error), success => Right(success)))
  }

  def interceptSqlErrors[T](zio: ZIO[Any, SQLException, T]): ZIO[Any, ErrorInfo, T] = {
    zio.mapError { error =>
      logger.error(s"Intercepted SQL exception", error)
      InternalServerError("Internal error")
    }
  }
}

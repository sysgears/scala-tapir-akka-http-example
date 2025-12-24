package com.example.utils

import zio.{UIO, Unsafe, ZIO}

import scala.concurrent.Future

object ZioUtil {


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
}

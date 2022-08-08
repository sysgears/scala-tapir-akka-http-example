package com.example.utils

import scala.concurrent.{ExecutionContext, Future}

object Util {

  /**
   * Function, which converts either with future to future with either.
   * @param e target either
   * @param ec execution context for future convertion
   * @tparam A error type
   * @tparam B success type
   * @return future with either.
   */
  def foldEitherOfFuture[A, B](e: Either[A, Future[B]])(implicit ec: ExecutionContext): Future[Either[A, B]] =
    e match {
      case Left(s) => Future.successful(Left(s))
      case Right(f) => f.map(Right(_))
    }
}

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

  /** Email regex. Taken from Play forms. */
  private val emailRegex = """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r

  /** Validates text on email format. */
  def isTextEmail(text: String): Boolean = {
    emailRegex.matches(text)
  }
}

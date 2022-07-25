package com.example.utils

import scala.concurrent.{ExecutionContext, Future}

object Util {

  def foldEitherOfFuture[A, B](e: Either[A, Future[B]])(implicit ec: ExecutionContext): Future[Either[A, B]] =
    e match {
      case Left(s) => Future.successful(Left(s))
      case Right(f) => f.map(Right(_))
    }
}

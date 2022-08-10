package com.example.errors

/**
 * Trait for custom http code handling for error cases.
 */
sealed trait ErrorInfo {
  val msg: String
}

/** Represents http 404. */
case class NotFound(msg: String) extends ErrorInfo

/** Represents http 401. */
case class Unauthorized(msg: String) extends ErrorInfo

/** Represents http 403. */
case class Forbidden(msg: String) extends ErrorInfo

/** Represents http 409. */
case class Conflict(msg: String) extends ErrorInfo

/** Default case. */
case class ErrorMessage(msg: String) extends ErrorInfo

/** Represents http 400. */
case class BadRequest(msg: String) extends ErrorInfo

/** Represents http 500. */
case class InternalServerError(msg: String) extends ErrorInfo


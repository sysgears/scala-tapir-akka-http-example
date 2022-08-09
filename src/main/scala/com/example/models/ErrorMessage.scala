package com.example.models

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/**
 * Response format for error
 * @param msg error response message.
 */
case class ErrorMessage(msg: String)

object ErrorMessage {
  /** Circe json format. */
  implicit val errorMessageDecoder = deriveDecoder[ErrorMessage]
  implicit val errorMessageEncoder = deriveEncoder[ErrorMessage]
}

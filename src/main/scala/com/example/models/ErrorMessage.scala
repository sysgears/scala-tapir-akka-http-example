package com.example.models

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class ErrorMessage(msg: String)

object ErrorMessage {
  implicit val errorMessageDecoder = deriveDecoder[ErrorMessage]
  implicit val errorMessageEncoder = deriveEncoder[ErrorMessage]
}

package com.example.models

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class AuthError(msg: String)

object AuthError {
  implicit val authErrorDecoder = deriveDecoder[AuthError]
  implicit val authErrorEncoder = deriveEncoder[AuthError]
}

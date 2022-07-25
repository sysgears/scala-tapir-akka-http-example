package com.example.models

import java.time.LocalDateTime

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class User(id: Long,
                name: String,
                phoneNumber: String,
                email: String,
                passwordHash: String,
                zip: String,
                city: String,
                address: String,
                role: String,
                created: LocalDateTime)

object User {
  import com.example.utils.GenericJsonFormats._
  implicit val userDecoder = deriveDecoder[User]
  implicit val userEncoder = deriveEncoder[User]
}

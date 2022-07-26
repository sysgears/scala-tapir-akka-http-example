package com.example.models

import java.time.LocalDateTime

import com.example.models.Roles.RoleType
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class User(id: Long,
                name: String,
                phoneNumber: String,
                email: String,
                passwordHash: String,
                zip: String,
                city: String,
                address: String,
                role: RoleType,
                created: LocalDateTime)

object User {
  import com.example.utils.GenericJsonFormats._
  import Roles.{decoderFormat, encoderFormat}
  implicit val userDecoder = deriveDecoder[User]
  implicit val userEncoder = deriveEncoder[User]
}

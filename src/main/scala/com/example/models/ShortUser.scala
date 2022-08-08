package com.example.models

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class ShortUser(id: Long,
                     name: String,
                     phoneNumber: String,
                     email: String,
                     zip: String,
                     city: String,
                     address: String)

object ShortUser {
  implicit val userDecoder = deriveDecoder[ShortUser]
  implicit val userEncoder = deriveEncoder[ShortUser]

  def apply(user: User): ShortUser = {
    ShortUser(user.id, user.name, user.phoneNumber, user.email, user.zip, user.city, user.address)
  }
}

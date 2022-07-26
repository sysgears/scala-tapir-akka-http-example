package com.example.models

import io.circe.{Decoder, Encoder}

import scala.util.Try

object Roles extends Enumeration {
  type RoleType = Value

  val Admin: RoleType = Value(2)
  val User: RoleType =  Value(1)

  def withId(s: Int): Value = values.find(t => t.id ==s ).head

  implicit val encoderFormat = Encoder.encodeInt.contramap[RoleType](_.id)
  implicit val decoderFormat = Decoder.decodeInt.emap[RoleType](int => Try(withId(int)).toEither.left.map(_.getMessage))
}

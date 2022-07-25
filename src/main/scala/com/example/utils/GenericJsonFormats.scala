package com.example.utils

import java.time.{Instant, LocalDateTime}
import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}

import io.circe.{Decoder, Encoder}

import scala.util.Try

object GenericJsonFormats {

  val formatter = new DateTimeFormatterBuilder().append(DateTimeFormatter.ISO_LOCAL_DATE)
    .appendLiteral('T')
    .append(DateTimeFormatter.ISO_LOCAL_TIME)
    .toFormatter();
  implicit val dateEncoder = Encoder.encodeString.contramap[LocalDateTime](_.format(formatter))
  implicit val dateDecoder = Decoder.decodeString.emap[LocalDateTime](str => {
    Try(LocalDateTime.parse(str, formatter)).toEither.left.map(_.getMessage)
  })

  implicit val encodeInstant: Encoder[Instant] = Encoder.encodeString.contramap[Instant](_.toString)

  implicit val decodeInstant: Decoder[Instant] = Decoder.decodeString.emapTry { str =>
    Try(Instant.parse(str))
  }
}

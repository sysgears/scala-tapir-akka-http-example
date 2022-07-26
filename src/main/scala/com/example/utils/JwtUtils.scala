package com.example.utils

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.{Date, UUID}

import com.typesafe.config.Config
import io.jsonwebtoken.{Jwts, SignatureAlgorithm}

class JwtUtils(config: Config) {

  private val secret = config.getString("jwt.secret")
  private val ttlSeconds = config.getLong("jwt.expiration.seconds")

  def generateJwt(userId: Long): String = {
    val now = Instant.now
    val jwt = Jwts.builder()
      .setId(UUID.randomUUID.toString)
      .setIssuedAt(Date.from(now))
      .setExpiration(Date.from(now.plusSeconds(ttlSeconds)))
      .signWith(
        SignatureAlgorithm.HS512,
        secret.getBytes(StandardCharsets.UTF_8.toString)
      ).claim("userId", userId)
    jwt.compact()
  }
}

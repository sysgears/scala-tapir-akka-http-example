package com.example.auth

import com.example.auth
import com.example.utils.Util
import com.typesafe.config.Config
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.{Claims, Jwts}
import zio.{Task, UIO, ZIO, ZLayer}

import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.{Date, UUID}

/** Service, which works with jwt. */
object Jwt {

  type JwtService = Jwt.Service

  trait Service {
    def generateJwt(userId: String): UIO[String]

    /**
     * Extracts user id from jwt token
     * @param jwt token from authorization header.
     * @return optional user id. Or fails with exception
     */
    def extractUserIdFromJwt(jwt: String): Task[Option[String]]
  }

  val live = ZLayer {
    for {
      config <- ZIO.service[Config]
    } yield {
      new auth.Jwt.JwtService {
        /** Jwt token secret */
        private val secret = config.getString("jwt.secret")

        /** Jwt token live duration */
        private val ttlSeconds = config.getLong("jwt.expiration.seconds")

        override def generateJwt(userId: String): UIO[String] = {
          val now = Instant.now
          ZIO.attempt {
            val jwt = Jwts.builder()
              .id(UUID.randomUUID.toString) // id for jwt
              .issuedAt(Date.from(now)) // time from which token is active
              .expiration(Date.from(now.plusSeconds(ttlSeconds))) // time to which token is active
              .signWith( // signing jwt.
                Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8.toString))
              ).claim("userId", userId) // adding claim
            jwt.compact()
          }.orDie
        }


        override def extractUserIdFromJwt(jwt: String): Task[Option[String]] = {
          ZIO.attempt {
            val decodedJwtStr =
              URLDecoder.decode(jwt, StandardCharsets.UTF_8.toString)
            Jwts
              .parser()
              .verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8.toString)))
              .build()
              .parseSignedClaims(decodedJwtStr)
          } map { claims =>
            val jwtClaims: Claims = claims.getPayload
            Util.emptyStringToOption(jwtClaims.get("userId").toString)
          }
        }
      }
    }
  }
}

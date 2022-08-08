package com.example.auth

import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.{Date, UUID}

import com.example.dao.UserDao
import com.example.models.User
import com.typesafe.config.Config
import io.jsonwebtoken.{Claims, Jwts, SignatureAlgorithm}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/** Service, which works with jwt. */
class JwtService(config: Config, userDao: UserDao) {

  /** Jwt token secret */
  private val secret = config.getString("jwt.secret")

  /** Jwt token live duration */
  private val ttlSeconds = config.getLong("jwt.expiration.seconds")

  /** Generates jwt with user id in it. */
  def generateJwt(userId: Long): String = {
    val now = Instant.now
    val jwt = Jwts.builder()
      .setId(UUID.randomUUID.toString) // id for jwt
      .setIssuedAt(Date.from(now)) // time from which token is active
      .setExpiration(Date.from(now.plusSeconds(ttlSeconds))) // time to which token is active
      .signWith( // signing jwt.
        SignatureAlgorithm.HS512,
        secret.getBytes(StandardCharsets.UTF_8.toString)
      ).claim("userId", userId) // adding claim
    jwt.compact()
  }

  /**
   * Extracts user from jwt token
   * @param jwt token from authorization header.
   * @param ec for async futures.
   * @return either exception or optional user.
   */
  def extractUserFromJwt(jwt: String)(implicit ec: ExecutionContext): Future[Either[Throwable, Option[User]]] = {
    val decodedJwtStr =
      URLDecoder.decode(jwt, StandardCharsets.UTF_8.toString)
    Try {
      Jwts
        .parser()
        .setSigningKey(secret.getBytes(StandardCharsets.UTF_8.toString))
        .parseClaimsJws(decodedJwtStr)
    } match {
      case Failure(exception) => Future.successful(Left(exception))
      case Success(claims) =>
        val jwtClaims: Claims = claims.getBody
        jwtClaims.get("userId").toString.toLongOption match {
          case Some(userId) => userDao.find(userId).map(Right(_))
          case None => Future.successful(Right(None))
        }
    }
  }
}

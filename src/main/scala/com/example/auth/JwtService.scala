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

class JwtService(config: Config, userDao: UserDao) {

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

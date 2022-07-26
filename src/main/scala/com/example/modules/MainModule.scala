package com.example.modules

import akka.actor.ActorSystem
import com.example.auth.{JwtService, TapirAuthentication, TapirSecurity}
import com.example.controllers.AuthController
import com.example.dao.UserDao
import com.softwaremill.macwire._
import com.typesafe.config.{Config, ConfigFactory}
import io.getquill.{PostgresJdbcContext, SnakeCase}

trait MainModule {

  implicit val actorSystem: ActorSystem = ActorSystem()
  import actorSystem.dispatcher

  lazy val config: Config = ConfigFactory.load()
  lazy val ctx = new PostgresJdbcContext(SnakeCase, "db.default")
  lazy val userDao = wire[UserDao]
  lazy val jwtService = wire[JwtService]
  lazy val authentication = wire[TapirAuthentication]
  lazy val tapirSecurity = wire[TapirSecurity]
  lazy val authController = wire[AuthController]

}

package com.example.modules

import akka.actor.ActorSystem
import com.example.auth.{JwtService, TapirAuthentication, TapirSecurity}
import com.example.controllers.admin.AdminProductController
import com.example.controllers.{AuthController, OrderController}
import com.example.dao._
import com.softwaremill.macwire._
import com.typesafe.config.{Config, ConfigFactory}
import io.getquill.{PostgresJdbcContext, SnakeCase}

trait MainModule {

  implicit val actorSystem: ActorSystem = ActorSystem()
  import actorSystem.dispatcher

  lazy val config: Config = ConfigFactory.load()
  lazy val ctx = new PostgresJdbcContext(SnakeCase, "db.default")
  lazy val userDao          = wire[UserDao]
  lazy val orderDao         = wire[OrderDao]
  lazy val productDao       = wire[ProductDao]
  lazy val orderProductDao  = wire[OrderProductDao]
  lazy val jwtService       = wire[JwtService]
  lazy val authentication   = wire[TapirAuthentication]
  lazy val tapirSecurity    = wire[TapirSecurity]
  lazy val authController   = wire[AuthController]
  lazy val orderController  = wire[OrderController]
  lazy val adminProductController = wire[AdminProductController]

}

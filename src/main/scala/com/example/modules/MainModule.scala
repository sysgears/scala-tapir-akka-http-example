package com.example.modules

import akka.actor.ActorSystem
import com.example.auth.{JwtService, TapirAuthentication, TapirSecurity}
import com.example.controllers.admin._
import com.example.controllers._
import com.example.dao._
import com.example.errors.ErrorHandler
import com.example.services._
import com.example.services.admin._
import com.example.utils.RequestTimeTracker
import com.softwaremill.macwire._
import com.typesafe.config.{Config, ConfigFactory}
import io.getquill.{PostgresJdbcContext, SnakeCase}

/**
 * Macwire module.
 *
 * Contains all classes for application's work.
 */
trait MainModule {

  implicit val actorSystem: ActorSystem = ActorSystem()
  import actorSystem.dispatcher

  lazy val config: Config = ConfigFactory.load() // for macwire not necessary to wire everything, having required component in scope is enough.
  lazy val ctx = new PostgresJdbcContext(SnakeCase, "db.default")
  lazy val userDao          = wire[UserDao]
  lazy val errorHandler     = wire[ErrorHandler]
  lazy val timeTracker      = wire[RequestTimeTracker]
  lazy val orderDao         = wire[OrderDao]
  lazy val productDao       = wire[ProductDao]
  lazy val orderProductDao  = wire[OrderProductDao]
  lazy val jwtService       = wire[JwtService]
  lazy val authentication   = wire[TapirAuthentication]
  lazy val tapirSecurity    = wire[TapirSecurity]
  lazy val authService      = wire[AuthService]
  lazy val orderService     = wire[OrderService]
  lazy val productService   = wire[ProductService]
  lazy val adminOrderService= wire[AdminOrderService]
  lazy val adminProductService = wire[AdminProductService]
  lazy val authController   = wire[AuthController]
  lazy val orderController  = wire[OrderController]
  lazy val productController  = wire[ProductController]
  lazy val adminProductController = wire[AdminProductController]
  lazy val adminOrderController = wire[AdminOrderController]

}

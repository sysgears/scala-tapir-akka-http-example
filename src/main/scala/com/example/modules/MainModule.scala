package com.example.modules

import akka.actor.ActorSystem
import com.example.auth.Jwt.JwtService
import com.example.auth.TapirAuthentication.TapirAuth
import com.example.auth.{Jwt, TapirAuthentication, TapirSecurity}
import com.example.controllers.admin._
import com.example.controllers._
import com.example.dao.OrderDao.OrderRepository
import com.example.dao.OrderProductDao.OrderProductRepository
import com.example.dao.ProductDao.ProductRepository
import com.example.dao.UserDao.UserRepository
import com.example.dao._
import com.example.errors.ErrorHandler
import com.example.services.AuthService.Authentication
import com.example.services.OrderService.OrdersService
import com.example.services.ProductService.ProductService
import com.example.services._
import com.example.services.admin.AdminOrderService.AdminOrders
import com.example.services.admin.AdminProductService.AdminProducts
import com.example.services.admin._
import com.example.utils.RequestTimeTracker
import com.typesafe.config.{Config, ConfigFactory}
import io.getquill.jdbczio.Quill
import io.getquill.{PostgresJdbcContext, SnakeCase}
import zio.ZLayer

import javax.sql.DataSource

/** Macwire module.
  *
  * Contains all classes for application's work.
  */
trait MainModule {

  implicit val actorSystem: ActorSystem = ActorSystem()
  import actorSystem.dispatcher

  // ZLayer layers
  lazy val config = ZLayer.succeed(ConfigFactory.load())

  lazy val postgres: ZLayer[DataSource, Nothing, Quill.Postgres[SnakeCase.type]] =
    Quill.Postgres.fromNamingStrategy(SnakeCase)

  lazy val dataSource: ZLayer[Any, Throwable, DataSource] =
    Quill.DataSource.fromPrefix("db.default")

  lazy val userDao: ZLayer[Quill.Postgres[SnakeCase], Nothing, UserRepository] =
    UserDao.live

  lazy val orderDao: ZLayer[Quill.Postgres[SnakeCase], Nothing, OrderRepository] =
    OrderDao.live

  lazy val productDao: ZLayer[Quill.Postgres[SnakeCase], Nothing, ProductRepository] =
    ProductDao.live

  lazy val orderProductDao: ZLayer[Quill.Postgres[SnakeCase], Nothing, OrderProductRepository] =
    OrderProductDao.live
  lazy val jwtService: ZLayer[Config, Nothing, JwtService] = Jwt.live

  lazy val tapirAuth: ZLayer[UserRepository with JwtService, Nothing, TapirAuth] =
    TapirAuthentication.live

  lazy val authService: ZLayer[JwtService with UserRepository, Nothing, Authentication] =
    AuthService.live

  lazy val orderService
      : ZLayer[OrderProductRepository with ProductRepository with OrderRepository, Nothing, OrdersService] =
    OrderService.live

  lazy val productService: ZLayer[ProductRepository, Nothing, ProductService] =
    ProductService.live

  lazy val adminProductService: ZLayer[ProductRepository, Nothing, AdminProducts] =
    AdminProductService.live

  lazy val adminOrderService: ZLayer[
    OrderProductRepository with UserRepository with ProductRepository with OrderRepository,
    Nothing,
    AdminOrders
  ] = AdminOrderService.live

  // ULayer creation. The reason - to compose layers into single one for convenient transfer to controllers
  // Layer wiring is done automatically, you just need to provide all required zlayers.
  // orDie forces to throw an error if anything happens in the app's start
  lazy val authenticationLayer: ZLayer[Any, Nothing, TapirAuth] = ZLayer
    .make[TapirAuth](
      tapirAuth,
      userDao,
      dataSource,
      postgres,
      jwtService,
      config
    )
    .orDie

  lazy val authServiceLayer: ZLayer[Any, Nothing, Authentication] = ZLayer
    .make[Authentication](
      authService,
      userDao,
      dataSource,
      postgres,
      jwtService,
      config
    )
    .orDie

  lazy val orderServiceLayer: ZLayer[Any, Nothing, OrdersService] = ZLayer
    .make[OrdersService](
      orderService,
      orderDao,
      orderProductDao,
      productDao,
      dataSource,
      postgres
    )
    .orDie

  lazy val productServiceLayer: ZLayer[Any, Nothing, ProductService] = ZLayer
    .make[ProductService](productService, productDao, dataSource, postgres)
    .orDie

  lazy val adminProductServiceLayer: ZLayer[Any, Nothing, AdminProducts] = ZLayer
    .make[AdminProducts](adminProductService, productDao, dataSource, postgres)
    .orDie

  lazy val adminOrderServiceLayer: ZLayer[Any, Nothing, AdminOrders] = ZLayer
    .make[AdminOrders](
      adminOrderService,
      orderDao,
      userDao,
      orderProductDao,
      productDao,
      dataSource,
      postgres
    )
    .orDie

  lazy val authController = new AuthController(authServiceLayer)
  lazy val tapirSecurity  = new TapirSecurity(authenticationLayer)

  lazy val orderController =
    new OrderController(tapirSecurity, orderServiceLayer)

  lazy val productController =
    new ProductController(tapirSecurity, productServiceLayer)

  lazy val adminProductController =
    new AdminProductController(tapirSecurity, adminProductServiceLayer)

  lazy val adminOrderController =
    new AdminOrderController(tapirSecurity, adminOrderServiceLayer)

  lazy val timeTracker  = new RequestTimeTracker()
  lazy val errorHandler = new ErrorHandler()

}

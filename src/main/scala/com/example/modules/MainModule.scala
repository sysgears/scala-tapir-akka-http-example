package com.example.modules

import akka.actor.ActorSystem
import com.example.auth.{TapirAuthentication, TapirSecurity}
import com.example.dao.UserDao
import com.softwaremill.macwire._
import io.getquill.{PostgresJdbcContext, SnakeCase}

trait MainModule {

  implicit val actorSystem: ActorSystem = ActorSystem()
  import actorSystem.dispatcher

  lazy val ctx = new PostgresJdbcContext(SnakeCase, "db.default")
  lazy val userDao = wire[UserDao]
  lazy val authentication = wire[TapirAuthentication]
  lazy val tapirSecurity = wire[TapirSecurity]

}

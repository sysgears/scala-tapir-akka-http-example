package com.example

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import com.typesafe.scalalogging.LazyLogging
import sttp.tapir.server.akkahttp.AkkaHttpServerInterpreter
import sttp.tapir._

import scala.concurrent.Future
import scala.io.StdIn

object TapirRoutes extends App with LazyLogging {

  implicit val actorSystem: ActorSystem = ActorSystem()
  import actorSystem.dispatcher

  case class User(name: String, role: String)
  def hasRole(user: User, role: String): Future[Either[Unit, User]] = Future.successful(if(user.role.equals(role)) Right(user) else Left(()) )
  def authenticate(token: String): Future[Either[Unit, User]] = Future(Right(User("Peter", "User")))

  val tapirSecurityEndpoint = endpoint.securityIn(auth.bearer[String]())
    .serverSecurityLogic(authenticate(_).flatMap(either => foldEitherOfFuture(either.map(hasRole(_, "Admin"))).map(_.flatten)))

  val tapirEndpoint = tapirSecurityEndpoint.get.in("test").out(stringBody)

  val route = AkkaHttpServerInterpreter().toRoute(tapirEndpoint.serverLogic { user => input =>
    Future(Right(s"test ok response with user $user"))
    /* here we can use both `special` and `input` values */
  })

  val bindingFuture = Http().newServerAt("localhost", 9000).bind(route)
  logger.info(s"Server online at http://localhost:9000/")
  logger.info("Press RETURN to stop...")
  StdIn.readLine()
  logger.info("Going offline....")
  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => actorSystem.terminate())

  def foldEitherOfFuture[A, B](e: Either[A, Future[B]]): Future[Either[A, B]] =
    e match {
      case Left(s) => Future.successful(Left(s))
      case Right(f) => f.map(Right(_))
    }
}

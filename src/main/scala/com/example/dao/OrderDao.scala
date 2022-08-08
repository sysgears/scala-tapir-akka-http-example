package com.example.dao

import com.example.models.Order
import io.getquill.NamingStrategy
import io.getquill.context.jdbc.JdbcContext
import io.getquill.context.sql.idiom.SqlIdiom

import scala.concurrent.{ExecutionContext, Future}

class OrderDao(context: JdbcContext[_ <: SqlIdiom, _ <: NamingStrategy])(implicit ec: ExecutionContext) {

  import context._

  private val orders = quote {
    querySchema[Order]("orders")
  }

  def insert(order: Order): Future[Long] = Future {
    run(orders.insertValue(lift(order)).returningGenerated(_.id))
  }

  def update(order: Order): Future[Long] = Future {
    run(orders.filter(_.id == lift(order.id)).updateValue(lift(order)))
  }

  def updateStatus(orderId: Long, newStatus: String): Future[Long] = Future {
    run(orders.filter(_.id == lift(orderId)).update(_.status -> lift(newStatus)))
  }

  def remove(orderId: Long): Future[Long] = Future {
    run(orders.filter(_.id == lift(orderId)).delete)
  }

  def find(orderId: Long): Future[Option[Order]] = Future {
    run(orders.filter(_.id == lift(orderId))).headOption
  }

  def findForUser(userId: Long): Future[List[Order]] = Future {
    run(orders.filter(_.userId == lift(userId)))
  }

  def findPaginated(take: Int, offset: Int): Future[List[Order]] = Future {
    run(orders.drop(lift(offset)).take(lift(take)))
  }

  def countOrders(): Future[Long] = Future {
    run(orders.size)
  }

}

package com.example.dao

import com.example.models.Order
import io.getquill.NamingStrategy
import io.getquill.context.jdbc.JdbcContext
import io.getquill.context.sql.idiom.SqlIdiom

import scala.concurrent.{ExecutionContext, Future}

/**
 * Dao for orders.
 *
 * @param context for running queries in database.
 * @param ec for running queries asynchronously.
 */
class OrderDao(context: JdbcContext[_ <: SqlIdiom, _ <: NamingStrategy])(implicit ec: ExecutionContext) {

  import context._

  /**
   * Query schema for orders.
   */
  private val orders = quote {
    querySchema[Order]("orders")
  }

  /** Inserts order to database and returns generated id of new order. */
  def insert(order: Order): Future[Long] = Future {
    run(orders.insertValue(lift(order)).returningGenerated(_.id))
  }

  /** Updates order. */
  def update(order: Order): Future[Long] = Future {
    run(orders.filter(_.id == lift(order.id)).updateValue(lift(order)))
  }

  /** Updates status for order. */
  def updateStatus(orderId: Long, newStatus: String): Future[Long] = Future {
    run(orders.filter(_.id == lift(orderId)).update(_.status -> lift(newStatus))) // example of updating some field for object in db.
  }

  /** Removed order from database. */
  def remove(orderId: Long): Future[Long] = Future {
    run(orders.filter(_.id == lift(orderId)).delete)
  }

  /** Retrieves order by it's id. */
  def find(orderId: Long): Future[Option[Order]] = Future {
    run(orders.filter(_.id == lift(orderId))).headOption
  }

  /** Retrieves orders for user */
  def findForUser(userId: Long): Future[List[Order]] = Future {
    run(orders.filter(_.userId == lift(userId)))
  }

  /** Retrieves paginated orders. */
  def findPaginated(take: Int, offset: Int): Future[List[Order]] = Future {
    run(orders.drop(lift(offset)).take(lift(take)))
  }

  /** Counts all orders. */
  def countOrders(): Future[Long] = Future {
    run(orders.size)
  }

}

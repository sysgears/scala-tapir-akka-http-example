package com.example.dao

import com.example.models.OrderProduct
import io.getquill.NamingStrategy
import io.getquill.context.jdbc.JdbcContext
import io.getquill.context.sql.idiom.SqlIdiom

import scala.concurrent.{ExecutionContext, Future}

/**
 * Dao for order-product relationship.
 *
 * @param context for running queries in database.
 * @param ec for running queries asynchronously.
 */
class OrderProductDao(context: JdbcContext[_ <: SqlIdiom, _ <: NamingStrategy])(implicit ec: ExecutionContext) {

  import context._

  /**
   * Query schema for orderProducts.
   */
  private val orderItems = quote {
    querySchema[OrderProduct]("order_items")
  }

  /** Creates orderProduct in database and returns generated id. */
  def insert(orderProduct: OrderProduct): Future[Long] = Future {
    run(orderItems.insertValue(lift(orderProduct)).returningGenerated(_.id))
  }

  /** Updates orderProduct relation. */
  def update(orderProduct: OrderProduct): Future[Long] = Future {
    run(orderItems.filter(_.id == lift(orderProduct.id)).updateValue(lift(orderProduct)))
  }

  /** Removes orderProduct relation. */
  def remove(orderProductId: Long): Future[Long] = Future {
    run(orderItems.filter(_.id == lift(orderProductId)).delete)
  }

  /** Retrieves order-product relations by order id list. */
  def findByOrders(orderIds: Seq[Long]): Future[List[OrderProduct]] = Future {
    run(orderItems.filter(orderItem => liftQuery(orderIds).contains(orderItem.orderId))) // example of batch extraction. liftQuery is required.
  }

  /** Retrieves order-product relations by order id. */
  def findByOrder(orderId: Long): Future[List[OrderProduct]] = Future {
    run(orderItems.filter(_.orderId == lift(orderId)))
  }

  def removeByOrder(orderId: Long): Future[List[OrderProduct]] = Future {
    run(orderItems.filter(_.orderId == lift(orderId)))
  }

  /** Batch order-product relation insert and returns generated id for each inserted entry. */
  def insertBatch(orderProductList: List[OrderProduct]): Future[List[Long]] = Future {
    run(liftQuery(orderProductList).foreach(entry => orderItems.insertValue(entry).returningGenerated(r => r.id))) // example of batch insert.
  }
}

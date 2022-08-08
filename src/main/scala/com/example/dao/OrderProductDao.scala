package com.example.dao

import com.example.models.OrderProduct
import io.getquill.NamingStrategy
import io.getquill.context.jdbc.JdbcContext
import io.getquill.context.sql.idiom.SqlIdiom

import scala.concurrent.{ExecutionContext, Future}

class OrderProductDao(context: JdbcContext[_ <: SqlIdiom, _ <: NamingStrategy])(implicit ec: ExecutionContext) {

  import context._

  private val orderItems = quote {
    querySchema[OrderProduct]("order_items")
  }

  def insert(orderProduct: OrderProduct): Future[Long] = Future {
    run(orderItems.insertValue(lift(orderProduct)).returningGenerated(_.id))
  }

  def update(orderProduct: OrderProduct): Future[Long] = Future {
    run(orderItems.filter(_.id == lift(orderProduct.id)).updateValue(lift(orderProduct)))
  }

  def remove(orderProductId: Long): Future[Long] = Future {
    run(orderItems.filter(_.id == lift(orderProductId)).delete)
  }

  def findByOrders(orderIds: Seq[Long]): Future[List[OrderProduct]] = Future {
    run(orderItems.filter(orderItem => liftQuery(orderIds).contains(orderItem.orderId)))
  }

  def findByOrder(orderId: Long): Future[List[OrderProduct]] = Future {
    run(orderItems.filter(_.orderId == lift(orderId)))
  }

  def removeByOrder(orderId: Long): Future[List[OrderProduct]] = Future {
    run(orderItems.filter(_.orderId == lift(orderId)))
  }

  def insertBatch(orderProductList: List[OrderProduct]): Future[List[Long]] = Future {
    run(liftQuery(orderProductList).foreach(entry => orderItems.insertValue(entry).returningGenerated(r => r.id)))
  }
}

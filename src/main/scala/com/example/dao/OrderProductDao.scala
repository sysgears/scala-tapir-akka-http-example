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

  def insert(order: OrderProduct): Future[Long] = Future {
    run(orderItems.insertValue(lift(order)).returningGenerated(_.id))
  }

  def update(order: OrderProduct): Future[Long] = Future {
    run(orderItems.filter(_.id == lift(order.id)).updateValue(lift(order)))
  }

  def remove(orderId: Long): Future[Long] = Future {
    run(orderItems.filter(_.id == lift(orderId)).delete)
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

package com.example.dao

import com.example.models.Product
import io.getquill.NamingStrategy
import io.getquill.context.jdbc.JdbcContext
import io.getquill.context.sql.idiom.SqlIdiom

import scala.concurrent.{ExecutionContext, Future}

class ProductDao(context: JdbcContext[_ <: SqlIdiom, _ <: NamingStrategy])(implicit ec: ExecutionContext) {

  import context._

  private val products = quote {
    querySchema[Product]("products")
  }

  def insert(order: Product): Future[Long] = Future {
    run(products.insertValue(lift(order)).returningGenerated(_.id))
  }

  def update(order: Product): Future[Long] = Future {
    run(products.filter(_.id == lift(order.id)).updateValue(lift(order)))
  }

  def remove(orderId: Long): Future[Long] = Future {
    run(products.filter(_.id == lift(orderId)).delete)
  }

  def findAll(): Future[List[Product]] = Future {
    run(products)
  }

  def findPaginated(take: Int, offset: Int): Future[List[Product]] = Future {
    run(products.drop(lift(offset)).take(lift(take)))
  }
}

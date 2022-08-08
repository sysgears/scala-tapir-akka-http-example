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

  def insert(product: Product): Future[Long] = Future {
    run(products.insertValue(lift(product)).returningGenerated(_.id))
  }

  def update(product: Product): Future[Long] = Future {
    run(products.filter(_.id == lift(product.id)).updateValue(lift(product)))
  }

  def remove(productId: Long): Future[Long] = Future {
    run(products.filter(_.id == lift(productId)).delete)
  }

  def findAll(): Future[List[Product]] = Future {
    run(products)
  }

  def findPaginated(take: Int, offset: Int): Future[List[Product]] = Future {
    run(products.drop(lift(offset)).take(lift(take)))
  }

  def countProducts(): Future[Long] = Future {
    run(products.size)
  }

  def findByIds(productIds: Seq[Long]): Future[List[Product]] = Future {
    run(products.filter(product => liftQuery(productIds).contains(product.id)))
  }
}

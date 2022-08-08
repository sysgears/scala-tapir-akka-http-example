package com.example.dao

import com.example.models.Product
import io.getquill.NamingStrategy
import io.getquill.context.jdbc.JdbcContext
import io.getquill.context.sql.idiom.SqlIdiom

import scala.concurrent.{ExecutionContext, Future}

/**
 * Dao for products.
 *
 * @param context runs queries in database
 * @param ec for running queries asynchronously.
 */
class ProductDao(context: JdbcContext[_ <: SqlIdiom, _ <: NamingStrategy])(implicit ec: ExecutionContext) {

  import context._

  /**
   * Query schema for products.
   */
  private val products = quote {
    querySchema[Product]("products")
  }

  /** Creates new product and returns generated id. */
  def insert(product: Product): Future[Long] = Future {
    run(products.insertValue(lift(product)).returningGenerated(_.id))
  }

  /** Updates product. */
  def update(product: Product): Future[Long] = Future {
    run(products.filter(_.id == lift(product.id)).updateValue(lift(product)))
  }

  /** Removes product. */
  def remove(productId: Long): Future[Long] = Future {
    run(products.filter(_.id == lift(productId)).delete)
  }

  /** Retrieves all products. */
  def findAll(): Future[List[Product]] = Future {
    run(products)
  }

  /** Retrieves products paginated. */
  def findPaginated(take: Int, offset: Int): Future[List[Product]] = Future {
    run(products.drop(lift(offset)).take(lift(take)))
  }

  /** Counts products in database. */
  def countProducts(): Future[Long] = Future {
    run(products.size)
  }

  /** Retrieves products for ids. */
  def findByIds(productIds: Seq[Long]): Future[List[Product]] = Future {
    run(products.filter(product => liftQuery(productIds).contains(product.id)))
  }
}

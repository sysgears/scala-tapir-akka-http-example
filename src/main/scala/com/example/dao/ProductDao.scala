package com.example.dao

import com.example.models.Product
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import zio.{ZIO, ZLayer}

import java.sql.SQLException

/**
 * Dao for products.
 */
object ProductDao {

  type ProductRepository = ProductDao.Service

  trait Service {
    def insert(product: Product): ZIO[Any, SQLException, Long]
    def update(product: Product): ZIO[Any, SQLException, Long]
    def remove(productId: String): ZIO[Any, SQLException, Long]
    def findAll(): ZIO[Any, SQLException, List[Product]]
    def findPaginated(take: Int, offset: Int): ZIO[Any, SQLException, List[Product]]
    def countProducts(): ZIO[Any, SQLException, Long]
    def findByIds(productIds: Seq[String]): ZIO[Any, SQLException, List[Product]]
  }

  val live = ZLayer {
    for {
      context <- ZIO.service[Quill.Postgres[SnakeCase]]
    } yield {
      new Service {
        import context._
        /**
         * Query schema for products.
         */
        private val products = quote {
          querySchema[Product]("products")
        }
        override def insert(product: Product): ZIO[Any, SQLException, Long] = run(products.insertValue(lift(product)))

        override def update(product: Product): ZIO[Any, SQLException, Long] = run(products.filter(_.id == lift(product.id)).updateValue(lift(product)))

        override def remove(productId: String): ZIO[Any, SQLException, Long] = run(products.filter(_.id == lift(productId)).delete)

        override def findAll(): ZIO[Any, SQLException, List[Product]] = run(products)

        override def findPaginated(take: Int, offset: Int): ZIO[Any, SQLException, List[Product]] = run(products.drop(lift(offset)).take(lift(take)))

        override def countProducts(): ZIO[Any, SQLException, Long] = run(products.size)

        override def findByIds(productIds: Seq[String]): ZIO[Any, SQLException, List[Product]] =
          run(products.filter(product => liftQuery(productIds).contains(product.id)))
      }
    }
  }
}

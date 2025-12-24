package com.example.dao

import com.example.models.Order
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import zio.{ZIO, ZLayer}

import java.sql.SQLException

/**
 * Dao for orders.
 *
 */
object OrderDao {

  type OrderRepository = OrderDao.Service

  trait Service {
    def findForUser(userId: String): ZIO[Any, SQLException, List[Order]]
    def find(orderId: String): ZIO[Any, SQLException, Option[Order]]

    /** Inserts order to database. */
    def insert(order: Order): ZIO[Any, SQLException, Long]
    def update(order: Order): ZIO[Any, SQLException, Long]
    def remove(orderId: String): ZIO[Any, SQLException, Long]
    def updateStatus(orderId: String, newStatus: String): ZIO[Any, SQLException, Long]

    /** Retrieves paginated orders. */
    def findPaginated(take: Int, offset: Int): ZIO[Any, SQLException, List[Order]]

    /** Counts all orders. */
    def countOrders(): ZIO[Any, SQLException, Long]
  }

  val live = ZLayer {
    for {
      context <- ZIO.service[Quill.Postgres[SnakeCase]]
    } yield {
      new Service {
        import context._

        /**
         * Query schema for orders.
         */
        private val orders = quote {
          querySchema[Order]("orders")
        }

        override def findForUser(userId: String): ZIO[Any, SQLException, List[Order]] = run(orders.filter(_.userId == lift(userId)))

        override def insert(order: Order): ZIO[Any, SQLException, Long] = run(orders.insertValue(lift(order)))

        override def update(order: Order): ZIO[Any, SQLException, Long] = run(orders.filter(_.id == lift(order.id)).updateValue(lift(order)))

        override def remove(orderId: String): ZIO[Any, SQLException, Long] =
          run(orders.filter(_.id == lift(orderId)).delete)

        override def updateStatus(orderId: String, newStatus: String): ZIO[Any, SQLException, Long] =
          run(orders.filter(_.id == lift(orderId)).update(_.status -> lift(newStatus))) // example of updating some field for object in db.

        /** Retrieves paginated orders. */
        override def findPaginated(take: Int, offset: Int): ZIO[Any, SQLException, List[Order]] =
          run(orders.drop(lift(offset)).take(lift(take)))

        /** Counts all orders. */
        override def countOrders(): ZIO[Any, SQLException, Long] =
          run(orders.size)

        override def find(orderId: String): ZIO[Any, SQLException, Option[Order]] = run(orders.filter(_.id == lift(orderId))).map(_.headOption)
      }
    }
  }

}

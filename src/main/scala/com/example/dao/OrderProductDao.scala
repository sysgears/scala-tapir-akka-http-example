package com.example.dao

import com.example.dao
import com.example.models.OrderProduct
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import zio.{ZIO, ZLayer}

import java.sql.SQLException

/**
  * Repository for OrderProduct table
  *
  * This is case of handling order-item relation for SQL databases.
  * In case of NoSQL, like MongoDB, you would keep information about item and quantity within order index
  * ElasticSearch would be a good spot too, but you still need to pair with a database. Some databases are not suitable for such relation, like Cassandra
  */
object OrderProductDao {

  type OrderProductRepository = OrderProductDao.Service

  trait Service {
    def insert(orderProduct: OrderProduct): ZIO[Any, SQLException, Long]

    def update(orderProduct: OrderProduct): ZIO[Any, SQLException, Long]

    def remove(orderId: String, productId: String): ZIO[Any, SQLException, Long]

    def findByOrders(
      orderIds: Seq[String]
    ): ZIO[Any, SQLException, List[OrderProduct]]

    def findByOrder(orderId: String): ZIO[Any, SQLException, List[OrderProduct]]

    def insertBatch(
      orderProductList: List[OrderProduct]
    ): ZIO[Any, SQLException, List[Long]]

    def removeByOrder(
      orderId: String
    ): ZIO[Any, SQLException, List[OrderProduct]]
  }

  val live: ZLayer[Quill.Postgres[SnakeCase],
                   Nothing,
                   dao.OrderProductDao.OrderProductRepository] = ZLayer {
    for {
      context <- ZIO.service[Quill.Postgres[SnakeCase]]
    } yield {
      new Service {

        import context._

        private val orderItems = quote {
          querySchema[OrderProduct]("order_products")
        }

        override def insert(
          orderProduct: OrderProduct
        ): ZIO[Any, SQLException, Long] =
          run(orderItems.insertValue(lift(orderProduct)))

        override def update(
          orderProduct: OrderProduct
        ): ZIO[Any, SQLException, Long] =
          run(
            orderItems
              .filter(
                orderItem =>
                  orderItem.orderId == lift(orderProduct.orderId) && orderItem.productId == lift(
                    orderProduct.productId
                )
              )
              .update(_.quantity -> lift(orderProduct.quantity))
          )

        override def remove(orderId: String,
                            productId: String): ZIO[Any, SQLException, Long] =
          run(
            orderItems
              .filter(
                orderItem =>
                  orderItem.orderId == lift(orderId) && orderItem.productId == lift(
                    productId
                )
              )
              .delete
          )

        override def findByOrders(
          orderIds: Seq[String]
        ): ZIO[Any, SQLException, List[OrderProduct]] =
          run(
            orderItems.filter(
              orderItem => liftQuery(orderIds).contains(orderItem.orderId)
            )
          ) // example of batch extraction. liftQuery is required.

        override def findByOrder(
          orderId: String
        ): ZIO[Any, SQLException, List[OrderProduct]] =
          run(orderItems.filter(_.orderId == lift(orderId)))

        override def insertBatch(
          orderProductList: List[OrderProduct]
        ): ZIO[Any, SQLException, List[Long]] =
          run(
            liftQuery(orderProductList)
              .foreach(entry => orderItems.insertValue(entry))
          ) // example of batch insert.

        override def removeByOrder(
          orderId: String
        ): ZIO[Any, SQLException, List[OrderProduct]] =
          run(orderItems.filter(_.orderId == lift(orderId)))
      }
    }
  }
}

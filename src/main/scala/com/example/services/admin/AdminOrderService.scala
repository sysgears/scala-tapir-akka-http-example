package com.example.services.admin

import com.example.dao.OrderDao.OrderRepository
import com.example.dao.OrderProductDao.OrderProductRepository
import com.example.dao.ProductDao.ProductRepository
import com.example.dao.UserDao.UserRepository
import com.example.errors.{ErrorInfo, InternalServerError}
import com.example.models.forms.{AdminOrderStatusChangeArguments, PaginatedEndpointArguments}
import com.example.models._
import com.example.utils.ZioUtil
import com.typesafe.scalalogging.LazyLogging
import zio.{ZIO, ZLayer}

object AdminOrderService extends LazyLogging {

  type AdminOrder = AdminOrderService.Service

  trait Service {
    def extractPaginatedOrders(args: PaginatedEndpointArguments): ZIO[Any, ErrorInfo, AdminOrderViewResponse]
    def updateOrderStatus(args: AdminOrderStatusChangeArguments): ZIO[Any, ErrorInfo, Long]
    def deleteOrder(orderId: String): ZIO[Any, ErrorInfo, Long]
  }

  val live = ZLayer {
    for {
      orderDao <- ZIO.service[OrderRepository]
      productDao <- ZIO.service[ProductRepository]
      userDao <- ZIO.service[UserRepository]
      orderProductDao <- ZIO.service[OrderProductRepository]
    } yield {
      new Service {
        override def extractPaginatedOrders(args: PaginatedEndpointArguments): ZIO[Any, ErrorInfo, AdminOrderViewResponse] = {
          logger.trace(s"Started extracting paginated orders, page: ${args.page}, page size: ${args.pageSize}")
          val offset = (args.page - 1) * args.pageSize
          val findPaginatedFuture = orderDao.findPaginated(args.pageSize, offset)
          val countOrdersFuture = orderDao.countOrders()
          ZioUtil.interceptSqlErrors(for {
            orders <- findPaginatedFuture
            orderCount <- countOrdersFuture
            orderProducts <- orderProductDao.findByOrders(orders.map(_.id))
            products <- productDao.findByIds(orderProducts.map(_.productId).distinct)
            users <- userDao.findByIds(orders.map(_.userId).distinct)
          } yield {
            val userOrders = orders.map { order =>
              val extendedOrderProducts = orderProducts.filter(_.orderId == order.id).map { orderProduct =>
                val product = products.find(_.id == orderProduct.productId)
                OrderRecord(product, orderProduct.quantity)
              }
              val user = users.find(_.id == order.userId)
              UserOrder(user.map(ShortUser(_)), OrderWithRecords(order, extendedOrderProducts))
            }
            val pages = (orderCount.toDouble / args.pageSize.toDouble).ceil.toInt // calculating amount of available pages
            val metadata = PaginationMetadata(args.page, args.pageSize, pages, orderCount)
            logger.debug(s"Extracted paginated orders, pack size: ${userOrders.size}, response metadata: $metadata")
            AdminOrderViewResponse(metadata, userOrders)
          })
        }

        override def updateOrderStatus(args: AdminOrderStatusChangeArguments): ZIO[Any, ErrorInfo, Long] =
          ZioUtil.interceptSqlErrors(orderDao.updateStatus(args.orderId, args.newStatus.toLowerCase()))

        override def deleteOrder(orderId: String): ZIO[Any, ErrorInfo, Long] = {
          ZioUtil.interceptSqlErrors(orderDao.remove(orderId))
        }
      }
    }
  }

}

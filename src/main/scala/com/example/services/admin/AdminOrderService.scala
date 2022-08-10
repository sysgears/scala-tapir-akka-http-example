package com.example.services.admin

import com.example.dao.{OrderDao, OrderProductDao, ProductDao, UserDao}
import com.example.models.{AdminOrderViewResponse, OrderRecord, OrderWithRecords, PaginationMetadata, ShortUser, UserOrder}
import com.example.models.forms.{AdminOrderStatusChangeArguments, PaginatedEndpointArguments}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{ExecutionContext, Future}

class AdminOrderService(orderDao: OrderDao,
                        productDao: ProductDao,
                        userDao: UserDao,
                        orderProductDao: OrderProductDao)(implicit ec: ExecutionContext) extends LazyLogging {

  def extractPaginatedOrders(args: PaginatedEndpointArguments): Future[AdminOrderViewResponse] = {
    logger.trace(s"Started extracting paginated orders, page: ${args.page}, page size: ${args.pageSize}")
    val offset = (args.page - 1) * args.pageSize
    val findPaginatedFuture = orderDao.findPaginated(args.pageSize, offset)
    val countOrdersFuture = orderDao.countOrders()
    for {
      orders <- findPaginatedFuture
      orderCount <- countOrdersFuture
      orderProducts <- orderProductDao.findByOrders(orders.map(_.id))
      products <- productDao.findByIds(orderProducts.map(_.productId).distinct)
      users <- userDao.findByIds(orders.map(_.userId).distinct)
    } yield {
      val userOrders = orders.map { order =>
        val extendedOrderProducts = orderProducts.filter(_.orderId == order.id).map { orderProduct =>
          val product = products.find(_.id == orderProduct.productId)
          OrderRecord(orderProduct.id, orderProduct.orderId, product, orderProduct.quantity)
        }
        val user = users.find(_.id == order.userId)
        UserOrder(user.map(ShortUser(_)), OrderWithRecords(order, extendedOrderProducts))
      }
      val pages = (orderCount.toDouble / args.pageSize.toDouble).ceil.toInt // calculating amount of available pages
      val metadata = PaginationMetadata(args.page, args.pageSize, pages, orderCount)
      logger.debug(s"Extracted paginated orders, pack size: ${userOrders.size}, response metadata: $metadata")
      AdminOrderViewResponse(metadata, userOrders)
    }
  }

  def updateOrderStatus(args: AdminOrderStatusChangeArguments): Future[Long] = {
    orderDao.updateStatus(args.orderId, args.newStatus.toLowerCase())
  }

  def deleteOrder(orderId: Long): Future[Long] = {
    orderDao.remove(orderId)
  }

}

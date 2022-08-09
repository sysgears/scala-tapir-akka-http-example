package com.example.services

import java.time.LocalDateTime

import com.example.dao.{OrderDao, OrderProductDao, ProductDao}
import com.example.models.{Order, OrderProduct, OrderRecord, OrderWithRecords}
import com.example.models.forms.CreateOrderForm

import scala.concurrent.{ExecutionContext, Future}

/**
 * Service for the OrderController.
 *
 * Contains functions, required for the controller's endpoints.
 *
 * @param orderDao dao for orders
 * @param productDao dao for products
 * @param orderProductDao dao for order-product relation
 * @param ec for futures.
 */
class OrderService(orderDao: OrderDao,
                   productDao: ProductDao,
                   orderProductDao: OrderProductDao)(implicit ec: ExecutionContext) {

  /**
   * Creates new order and associate it to the user.
   *
   * @param userId user which made the order
   * @param newOrder order itself.
   * @return insert result for order's records.
   */
  def createOrder(userId: Long, newOrder: CreateOrderForm): Future[List[Long]] = {
    val order = Order(0, userId, LocalDateTime.now(), Order.NEW_STATUS, LocalDateTime.now(), newOrder.comment)
    val products = newOrder.products.map(product => OrderProduct(0, 0, product.productId, product.quantity))
    for {
      orderId <- orderDao.insert(order)
      updatedProducts = products.map(_.copy(orderId = orderId))
      insertResult <- orderProductDao.insertBatch(updatedProducts)
    } yield {
      insertResult
    }
  }

  /**
   * Extracts orders for the user without their details.
   *
   * @param userId user, which extracts their orders.
   * @return orders for the user.
   */
  def findOrdersForUser(userId: Long): Future[List[Order]] = {
    orderDao.findForUser(userId)
  }

  /**
   * Extracts order details for the order.
   *
   * @param orderId order to extract records data.
   * @return extended order with records, which contains order details.
   */
  def getOrderDetails(orderId: Long): Future[Option[OrderWithRecords]] = {
    orderDao.find(orderId).flatMap {
      case Some(order) =>
        for {
          orderProducts <- orderProductDao.findByOrder(order.id)
          products <- productDao.findByIds(orderProducts.map(_.productId).distinct)
        } yield {
          val extendedOrderProducts = orderProducts.map { orderProduct =>
            val product = products.find(_.id == orderProduct.productId)
            OrderRecord(orderProduct.id, orderProduct.orderId, product, orderProduct.quantity)
          }
          Some(OrderWithRecords(order, extendedOrderProducts))
        }
      case None => Future.successful(None)
    }
  }
}

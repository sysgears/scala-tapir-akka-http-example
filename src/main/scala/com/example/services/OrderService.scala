package com.example.services

import com.example.dao.OrderDao.OrderRepository
import com.example.dao.OrderProductDao.OrderProductRepository
import com.example.dao.ProductDao.ProductRepository
import com.example.errors.{BadRequest, ErrorInfo, InternalServerError, NotFound}
import com.example.models.forms.CreateOrderForm
import com.example.models.{Order, OrderProduct, OrderRecord, OrderWithRecords}
import com.example.utils.Util
import com.typesafe.scalalogging.LazyLogging
import zio.{ZIO, ZLayer}

import java.time.LocalDateTime

/**
 * Service for the OrderController.
 *
 * Contains functions, required for the controller's endpoints.
 */
object OrderService extends LazyLogging {

  type OrderService = Service

  trait Service {

    /**
     * Creates new order and associate it to the user.
     *
     * @param userId user which made the order
     * @param newOrder order itself.
     */
    def createOrder(userId: String, newOrder: CreateOrderForm): ZIO[Any, ErrorInfo, Unit]

    /**
     * Extracts orders for the user without their details.
     *
     * @param userId user, which extracts their orders.
     * @return orders for the user.
     */
    def findOrdersForUser(userId: String): ZIO[Any, ErrorInfo, List[Order]]

    /**
     * Extracts order details for the order.
     *
     * @param orderId order to extract records data.
     * @return extended order with records, which contains order details.
     */
    def getOrderDetails(orderId: String): ZIO[Any, ErrorInfo, OrderWithRecords]
  }

  def createOrder(userId: String, newOrder: CreateOrderForm): ZIO[OrderService, ErrorInfo, Unit] = ZIO.serviceWithZIO[OrderService](_.createOrder(userId, newOrder))

  def findOrdersForUser(userId: String): ZIO[OrderService, ErrorInfo, List[Order]] = ZIO.serviceWithZIO[OrderService](_.findOrdersForUser(userId))

  def getOrderDetails(orderId: String): ZIO[OrderService, ErrorInfo, OrderWithRecords] = ZIO.serviceWithZIO[OrderService](_.getOrderDetails(orderId))

  val live = ZLayer {
    for {
      orderDao <- ZIO.service[OrderRepository]
      productDao <- ZIO.service[ProductRepository]
      orderProductDao <- ZIO.service[OrderProductRepository]
    } yield {
      new Service {
        override def createOrder(userId: String, newOrder: CreateOrderForm): ZIO[Any, ErrorInfo, Unit] = {
          val order = Order(Util.generateUuid, userId, LocalDateTime.now(), Order.NEW_STATUS, LocalDateTime.now(), newOrder.comment)
          val products = newOrder.products.map(product => OrderProduct(order.id, product.productId, product.quantity))
          (for {
            _ <- orderDao.insert(order)
            updatedProducts = products.map(_.copy(orderId = order.id))
            insertResult <- orderProductDao.insertBatch(updatedProducts)
          } yield {
            logger.debug(s"Order with id ${order.id} has been created.")
            ()
          }).mapError {
            error =>
              logger.error(s"Intercepted error while creating order for user $userId, order id is $userId", error)
              InternalServerError("Internal error")
          }
        }

        override def findOrdersForUser(userId: String): ZIO[Any, ErrorInfo, List[Order]] = {
          logger.debug(s"Received request to extract orders for user with id $userId")
          orderDao.findForUser(userId).mapError {
            error =>
              logger.error(s"Intercepted error from getting order details action, order id is $userId", error)
              InternalServerError("Internal error")
          }
        }


        /**
         * Extracts order details for the order.
         *
         * @param orderId order to extract records data.
         * @return extended order with records, which contains order details.
         */
        def getOrderDetails(orderId: String): ZIO[Any, ErrorInfo, OrderWithRecords] = {
          logger.trace(s"Received request to extract details for order with id $orderId")
          Util.emptyStringToOption(orderId) match {
            case Some(value) =>
              orderDao.find(value).flatMap {
                case Some(order) =>
                  for {
                    orderItems <- orderProductDao.findByOrder(order.id)
                    items <- productDao.findByIds(orderItems.map(_.productId).distinct)
                  } yield {
                    val extendedOrderProducts = orderItems.map { orderProduct =>
                      val product = items.find(_.id == orderProduct.productId)
                      OrderRecord(product, orderProduct.quantity)
                    }
                    logger.debug(s"Extracted details for order with id $orderId, extracted order records size ${extendedOrderProducts.size}")
                    OrderWithRecords(order, extendedOrderProducts)
                  }
                case None => ZIO.fail(NotFound())
              }.mapError {
                case error: ErrorInfo => error // pass this one
                case error =>
                  logger.error(s"Intercepted error from getting order details action, order id is $orderId", error)
                  InternalServerError("Internal error")
              }
            case None => ZIO.fail(NotFound("Order id is empty"))
          }

        }
      }
    }
  }
}

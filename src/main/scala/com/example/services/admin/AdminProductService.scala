package com.example.services.admin

import com.example.dao.ProductDao.ProductRepository
import com.example.errors.{ErrorInfo, InternalServerError}
import com.example.models.Product
import com.example.models.forms.NewProductForm
import com.example.utils.{Util, ZioUtil}
import com.typesafe.scalalogging.LazyLogging
import zio.{ZIO, ZLayer}

/**
 * Contains functions for the controller.
 */
object AdminProductService extends LazyLogging {

  type AdminProducts = AdminProductService.Service

  trait Service {
    def findAllProducts(): ZIO[Any, ErrorInfo, List[Product]]
    def insert(newProductForm: NewProductForm): ZIO[Any, ErrorInfo, Long]
    def update(product: Product): ZIO[Any, ErrorInfo, Long]
    def remove(productId: String): ZIO[Any, ErrorInfo, Long]
  }

  val live = ZLayer {
    for {
      productDao <- ZIO.service[ProductRepository]
    } yield {
      new Service {
        override def findAllProducts(): ZIO[Any, ErrorInfo, List[Product]] = {
          logger.debug("Extracting all products for admin page.")
          ZioUtil.interceptSqlErrors(productDao.findAll())
        }

        override def insert(newProductForm: NewProductForm): ZIO[Any, ErrorInfo, Long] = {
          val newProduct = Product(Util.generateUuid, newProductForm.name, newProductForm.description, newProductForm.price)
          logger.debug(s"Inserting new product $newProduct")
          ZioUtil.interceptSqlErrors(productDao.insert(newProduct))
        }

        override def update(product: Product): ZIO[Any, ErrorInfo, Long] = {
          logger.debug(s"Updating product $product")
          ZioUtil.interceptSqlErrors(productDao.update(product))
        }

        override def remove(productId: String): ZIO[Any, ErrorInfo, Long] = {
          logger.debug(s"Removing product with id $productId")
          ZioUtil.interceptSqlErrors(productDao.remove(productId))
        }
      }
    }
  }
}

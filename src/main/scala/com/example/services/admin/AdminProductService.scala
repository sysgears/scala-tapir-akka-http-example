package com.example.services.admin

import com.example.dao.ProductDao.ProductRepository
import com.example.errors.{ErrorInfo, InternalServerError, NotFound}
import com.example.models.Product
import com.example.models.forms.NewProductForm
import com.example.services.admin
import com.example.utils.{Util, ZioUtil}
import com.typesafe.scalalogging.LazyLogging
import zio.{ZIO, ZLayer}

/** Contains functions for the controller.
  */
object AdminProductService extends LazyLogging {

  type AdminProducts = AdminProductService.Service

  trait Service {
    def findAllProducts(): ZIO[Any, ErrorInfo, List[Product]]
    def insert(newProductForm: NewProductForm): ZIO[Any, ErrorInfo, Unit]
    def update(product: Product): ZIO[Any, ErrorInfo, String]
    def remove(productId: String): ZIO[Any, ErrorInfo, Unit]
  }

  def findAllProducts(): ZIO[AdminProducts, ErrorInfo, List[Product]] =
    ZIO.serviceWithZIO[AdminProducts](_.findAllProducts())

  def insert(
      newProductForm: NewProductForm
  ): ZIO[AdminProducts, ErrorInfo, Unit] =
    ZIO.serviceWithZIO[AdminProducts](_.insert(newProductForm))

  def update(product: Product): ZIO[AdminProducts, ErrorInfo, String] =
    ZIO.serviceWithZIO[AdminProducts](_.update(product))

  def remove(productId: String): ZIO[AdminProducts, ErrorInfo, Unit] =
    ZIO.serviceWithZIO[AdminProducts](_.remove(productId))

  val live: ZLayer[ProductRepository, Nothing, admin.AdminProductService.AdminProducts] = ZLayer {
    for {
      productDao <- ZIO.service[ProductRepository]
    } yield {
      new Service {
        override def findAllProducts(): ZIO[Any, ErrorInfo, List[Product]] = {
          logger.debug("Extracting all products for admin page.")
          ZioUtil.interceptSqlErrors(productDao.findAll())
        }

        override def insert(
            newProductForm: NewProductForm
        ): ZIO[Any, ErrorInfo, Unit] = {
          val newProduct = Product(
            Util.generateUuid,
            newProductForm.name,
            newProductForm.description,
            newProductForm.price
          )
          logger.debug(s"Inserting new product $newProduct")
          ZioUtil.interceptSqlErrors(productDao.insert(newProduct)).map(_ => ())
        }

        override def update(product: Product): ZIO[Any, ErrorInfo, String] = {
          logger.debug(s"Updating product $product")
          ZioUtil.interceptSqlErrors(productDao.update(product)).flatMap {
            case 0 =>
              ZIO.fail(NotFound(s"Product ${product.id} not found")) // if record wasn't removed
            case x if x > 0 => ZIO.succeed("Updated!") // success
            case _ =>
              logger.error(
                s"Intercepted unusual case when response from database is less than 0, PUT /admin/products/${product.id} endpoint, update product: $product"
              )
              ZIO.fail(InternalServerError("Unknown error, got less 0 result")) // unexpected result
          }
        }

        override def remove(productId: String): ZIO[Any, ErrorInfo, Unit] = {
          logger.debug(s"Removing product with id $productId")
          ZioUtil.interceptSqlErrors(productDao.remove(productId)).flatMap {
            case 0 =>
              ZIO.fail(NotFound(s"Product $productId not found")) // if record wasn't removed
            case x if x > 0 => ZIO.succeed(()) // success
            case _ =>
              logger.error(
                s"Intercepted unusual case when response from database is less than 0, DELETE /admin/products/$productId endpoint, delete product with id: $productId"
              )
              ZIO.fail(InternalServerError("Unknown error, got less 0 result")) // unexpected result
          }
        }
      }
    }
  }
}

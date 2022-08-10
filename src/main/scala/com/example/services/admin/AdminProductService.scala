package com.example.services.admin

import com.example.dao.ProductDao
import com.example.models.Product
import com.example.models.forms.NewProductForm
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{ExecutionContext, Future}

/**
 * Contains functions for the controller.
 *
 * @param productDao dao for products.
 * @param ec for futures.
 */
class AdminProductService(productDao: ProductDao)(implicit ec: ExecutionContext) extends LazyLogging {

  /** Extracts all products. */
  def findAllProducts(): Future[List[Product]] = {
    logger.debug("Extracting all products for admin page.")
    productDao.findAll()
  }

  /** Insert new product to database. */
  def insert(newProductForm: NewProductForm): Future[Long] = {
    val newProduct = Product(0, newProductForm.name, newProductForm.description, newProductForm.price)
    logger.debug(s"Inserting new product $newProduct")
    productDao.insert(newProduct)
  }

  /** Updates product. */
  def update(product: Product): Future[Long] = {
    logger.debug(s"Updating product $product")
    productDao.update(product)
  }

  /** Removes product. */
  def remove(productId: Long): Future[Long] = {
    logger.debug(s"Removing product with id $productId")
    productDao.remove(productId)
  }
}

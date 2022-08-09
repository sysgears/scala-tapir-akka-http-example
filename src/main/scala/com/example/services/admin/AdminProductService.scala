package com.example.services.admin

import com.example.dao.ProductDao
import com.example.models.Product
import com.example.models.forms.NewProductForm

import scala.concurrent.{ExecutionContext, Future}

/**
 * Contains functions for the controller.
 *
 * @param productDao dao for products.
 * @param ec for futures.
 */
class AdminProductService(productDao: ProductDao)(implicit ec: ExecutionContext) {

  /** Extracts all products. */
  def findAllProducts(): Future[List[Product]] = {
    productDao.findAll()
  }

  /** Insert new product to database. */
  def insert(newProductForm: NewProductForm): Future[Long] = {
    productDao.insert(Product(0, newProductForm.name, newProductForm.description, newProductForm.price))
  }

  /** Updates product. */
  def update(product: Product): Future[Long] = {
    productDao.update(product)
  }

  /** Removes product. */
  def remove(productId: Long): Future[Long] = {
    productDao.remove(productId)
  }
}

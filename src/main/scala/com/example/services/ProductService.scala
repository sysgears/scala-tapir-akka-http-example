package com.example.services

import com.example.dao.ProductDao
import com.example.models.{PaginatedProductListViewResponse, PaginationMetadata}
import com.example.models.forms.PaginatedEndpointArguments
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{ExecutionContext, Future}

/**
 * Service for product controller.
 *
 * Contains functions, required for the controller's endpoints.
 *
 * @param productDao dao for products.
 * @param ec for futures.
 */
class ProductService(productDao: ProductDao)(implicit ec: ExecutionContext) extends LazyLogging {

  /**
   * Extracts paginated products.
   *
   * @param args contains page and page size.
   * @return metadata and extracted products.
   */
  def extractPaginatedProducts(args: PaginatedEndpointArguments): Future[PaginatedProductListViewResponse] = {
    logger.trace(s"Started extracting paginated products, page: ${args.page}, page size: ${args.pageSize}")
    val offset = (args.page - 1) * args.pageSize
    val findPaginatedFuture = productDao.findPaginated(args.pageSize, offset)
    val countProductsFuture = productDao.countProducts()
    for {
      products <- findPaginatedFuture
      productsCount <- countProductsFuture
    } yield {
      val pages = (productsCount.toDouble / args.pageSize.toDouble).ceil.toInt
      val metadata = PaginationMetadata(args.page, args.pageSize, pages, productsCount)
      logger.debug(s"Extracted paginated orders, extracted pack size: ${products.size}, response metadata: $metadata")
      PaginatedProductListViewResponse(metadata, products)
    }
  }
}

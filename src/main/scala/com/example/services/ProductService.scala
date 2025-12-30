package com.example.services

import com.example.dao.ProductDao.ProductRepository
import com.example.errors.{ErrorInfo, InternalServerError}
import com.example.models.forms.PaginatedEndpointArguments
import com.example.models.{PaginatedProductListViewResponse, PaginationMetadata}
import com.example.services
import com.typesafe.scalalogging.LazyLogging
import zio.{ZIO, ZLayer}

/**
  * Service for product controller.
  *
  * Contains functions, required for the controller's endpoints.
  */
object ProductService extends LazyLogging {

  type ProductService = Service

  trait Service {

    /**
      * Extracts paginated products.
      *
      * @param args contains page and page size.
      * @return metadata and extracted products.
      */
    def extractPaginatedProducts(
      args: PaginatedEndpointArguments
    ): ZIO[Any, ErrorInfo, PaginatedProductListViewResponse]
  }

  def extractPaginatedProducts(
    args: PaginatedEndpointArguments
  ): ZIO[ProductService, ErrorInfo, PaginatedProductListViewResponse] =
    ZIO.serviceWithZIO[ProductService](_.extractPaginatedProducts(args))

  val live: ZLayer[ProductRepository,
                   Nothing,
                   services.ProductService.ProductService] = ZLayer {
    for {
      productDao <- ZIO.service[ProductRepository]
    } yield {
      new Service {

        /**
          * Extracts paginated products.
          *
          * @param args contains page and page size.
          * @return metadata and extracted products.
          */
        override def extractPaginatedProducts(
          args: PaginatedEndpointArguments
        ): ZIO[Any, ErrorInfo, PaginatedProductListViewResponse] = {
          logger.trace(
            s"Started extracting paginated products, page: ${args.page}, page size: ${args.pageSize}"
          )
          val offset = (args.page - 1) * args.pageSize
          val findPaginatedZio = productDao.findPaginated(args.pageSize, offset)
          val countProductsZio = productDao.countProducts()
          (for {
            products <- findPaginatedZio
            productsCount <- countProductsZio
          } yield {
            val pages =
              (productsCount.toDouble / args.pageSize.toDouble).ceil.toInt
            val metadata =
              PaginationMetadata(args.page, args.pageSize, pages, productsCount)
            logger.debug(
              s"Extracted paginated orders, extracted pack size: ${products.size}, response metadata: $metadata"
            )
            PaginatedProductListViewResponse(metadata, products)
          }).mapError { error =>
            logger.error(
              s"Intercepted error from extracting paginated products",
              error
            )
            InternalServerError("Internal error")
          }
        }
      }
    }
  }
}

package com.example.models

/** Response of product list view endpoint with metadata. */
case class PaginatedProductListViewResponse(metadata: PaginationMetadata, products: List[Product])

package com.example.models.forms

import sttp.tapir.EndpointIO.annotations.{description, query}

/** Contains query arguments for paginated endpoints. */
case class PaginatedEndpointArguments(
  @query
  @description("page number")
  page: Int = 1,
  @query
  @description("page's size")
  pageSize: Int = 10
)

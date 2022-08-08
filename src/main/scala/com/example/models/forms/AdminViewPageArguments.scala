package com.example.models.forms

import sttp.tapir.EndpointIO.annotations.query

case class AdminViewPageArguments(
  @query
  page: Int = 1,
  @query
  pageSize: Int = 10
)
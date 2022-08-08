package com.example.models.forms

import sttp.tapir.EndpointIO.annotations.{endpointInput, path, query}

@endpointInput("admin/orders/{orderId}")
case class AdminOrderStatusChangeArguments(
                                            @path
                                            orderId: Long,
                                            @query
                                            newStatus: String
                                          )

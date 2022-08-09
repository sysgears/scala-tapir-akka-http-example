package com.example.models.forms

import sttp.tapir.EndpointIO.annotations.{description, endpointInput, path, query}

/**
 * Arguments for change status admin endpoint.
 *
 * endpoint input annotation shows uri for the endpoint - /admin/orders/:orderId/status
 * @param orderId order id to update status
 * @param newStatus new status for the order.
 */
@endpointInput("admin/orders/{orderId}/status")
case class AdminOrderStatusChangeArguments(
                                            @path
                                            @description("id of order to update status")
                                            orderId: Long,
                                            @query
                                            @description("new status for the order.")
                                            newStatus: String
                                          )

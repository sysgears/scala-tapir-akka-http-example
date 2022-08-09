package com.example.models

/**
 * Admin paginated orders view response format
 * @param metadata contains pagination metadata
 * @param orders order with user, who made this order.
 */
case class AdminOrderViewResponse(metadata: PaginationMetadata, orders: List[UserOrder])

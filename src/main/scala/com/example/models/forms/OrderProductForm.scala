package com.example.models.forms

/**
 * Order record for create order request body.
 *
 * @param productId id of product to order.
 * @param quantity amount of products to order.
 */
case class OrderProductForm(productId: Long, quantity: Int)

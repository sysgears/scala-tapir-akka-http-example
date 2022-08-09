package com.example.models

/** Order-product relation. */
case class OrderProduct(id: Long, orderId: Long, productId: Long, quantity: Int)

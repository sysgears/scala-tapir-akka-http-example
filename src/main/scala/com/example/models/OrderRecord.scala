package com.example.models

/** Entity for order record with optional product (in case if product was removed.) */
case class OrderRecord(id: Long, orderId: Long, product: Option[Product], quantity: Int)

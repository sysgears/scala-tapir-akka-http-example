package com.example.models

/** Entity for order record with optional product (in case if product was removed.) */
case class OrderRecord(product: Option[Product], quantity: Int)

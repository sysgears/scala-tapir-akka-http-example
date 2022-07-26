package com.example.models

case class OrderRecord(id: Long, orderId: Long, product: Option[Product], quantity: Int)

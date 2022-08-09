package com.example.models

/** Contains order with it's records. */
case class OrderWithRecords(order: Order, orderRecords: List[OrderRecord])

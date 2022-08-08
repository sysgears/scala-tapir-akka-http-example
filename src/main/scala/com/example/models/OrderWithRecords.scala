package com.example.models

case class OrderWithRecords(order: Order, orderRecords: List[OrderRecord])

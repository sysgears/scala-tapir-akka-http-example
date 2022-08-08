package com.example.models

import java.time.LocalDateTime

case class Order(id: Long, userId: Long, created: LocalDateTime, status: String, lastUpdate: LocalDateTime, comment: String)

object Order {
  final val NEW_STATUS = "new"
  final val UNPROCESSED_STATUS = "unprocessed"
  final val PROCESSING_STATUS = "processing"
  final val COMPLETED_STATUS = "completed"

  final val appropriateStatuses = Seq(NEW_STATUS, UNPROCESSED_STATUS, PROCESSING_STATUS, COMPLETED_STATUS)
}

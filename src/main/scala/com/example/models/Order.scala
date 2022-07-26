package com.example.models

import java.time.LocalDateTime

case class Order(id: Long, userId: Long, created: LocalDateTime, status: String, lastUpdate: LocalDateTime)

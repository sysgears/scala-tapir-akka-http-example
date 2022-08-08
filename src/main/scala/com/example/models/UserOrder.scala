package com.example.models

/** Entity for order with records with user. */
case class UserOrder(user: Option[ShortUser], order: OrderWithRecords)

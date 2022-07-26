package com.example.models.forms

case class CreateOrderForm(products: List[OrderProductForm], comment: String)

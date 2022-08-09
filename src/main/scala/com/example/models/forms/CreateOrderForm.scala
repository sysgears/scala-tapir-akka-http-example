package com.example.models.forms

/**
 * Create order request format
 * @param products order records
 * @param comment order comment
 */
case class CreateOrderForm(products: List[OrderProductForm], comment: String)

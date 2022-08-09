package com.example.models.forms

/**
 * Request body format for create new product endpoint.
 *
 * @param name name of new product.
 * @param description new product description.
 * @param price new product price.
 */
case class NewProductForm(name: String, description: String, price: Double)

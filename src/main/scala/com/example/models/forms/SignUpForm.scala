package com.example.models.forms

case class SignUpForm(name: String,
                      phoneNumber: String,
                      email: String,
                      zip: String,
                      city: String,
                      address: String,
                      password: String,
                      repeatPassword: String)

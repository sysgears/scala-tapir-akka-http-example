package com.example.models.forms

import com.example.utils.Util

/**
 * Sign up request body format
 *
 * @param name new user's name
 * @param phoneNumber new user's phone number
 * @param email new user's email. Email has to be unique and not be present in database.
 * @param zip new user's zip code.
 * @param city new user's city where he's from.
 * @param address new user's address.
 * @param password new user's password
 * @param repeatPassword confirm password
 */
case class SignUpForm(name: String,
                      phoneNumber: String,
                      email: String,
                      zip: String,
                      city: String,
                      address: String,
                      password: String,
                      repeatPassword: String) {

  def isValid: Either[String, Unit] = {
    if (!Util.isTextEmail(email)) {
      Left("Invalid email format")
    } else if (!password.equals(repeatPassword)) {
      Left("Passwords not matches!")
    } else if (password.length < 6) {
      Left("Password is too short!")
    } else Right(())
  }
}

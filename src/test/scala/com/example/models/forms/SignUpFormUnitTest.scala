package com.example.models.forms

import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

/**
 * Tests for sign up form validation.
 */
class SignUpFormUnitTest extends AsyncFlatSpec with Matchers {

  val form: SignUpForm = SignUpForm("test name", "+77777777777", "test@example.com", "49050", "Dnipro", "test address, 46", "pass456", "pass456")

  /** Case when form is correct. */
  it should "validate correct form correctly" in {
    form.isValid.isRight shouldBe true
  }

  /** Case when form contains invalid email format. */
  it should "validate incorrect email correctly" in {
    form.copy(email = "test example.com").isValid.isLeft shouldBe true
  }

  /** Case when passwords are too short. */
  it should "validate too short password correctly" in {
    form.copy(password = "pass", repeatPassword = "pass").isValid.isLeft shouldBe true
  }

  /** Case when passwords not matches. */
  it should "validate not matching passwords correctly" in {
    form.copy(repeatPassword = "pass4567").isValid.isLeft shouldBe true
  }

}

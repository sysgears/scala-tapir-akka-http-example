package com.example.utils

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

object Util {

  /** Email regex. Taken from Play forms. */
  private val emailRegex =
    """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r

  /** Validates text on email format. */
  def isTextEmail(text: String): Boolean = {
    emailRegex.matches(text)
  }

  def generateUuid: String = UUID.randomUUID().toString

  def emptyStringToOption(string: String): Option[String] =
    Option(string).flatMap(_.trim match {
      case ""   => None
      case line => Some(line)
    })
}

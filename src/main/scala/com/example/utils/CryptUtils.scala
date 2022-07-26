package com.example.utils

import org.mindrot.jbcrypt.BCrypt

import scala.util.Try

object CryptUtils {

  def matchBcryptHash(candidate: String, hashed: String): Try[Boolean] = {
    Try {
      BCrypt.checkpw(candidate, hashed)
    }
  }

  def createBcryptHash(text: String): String = {
    BCrypt.hashpw(text, BCrypt.gensalt())
  }
}

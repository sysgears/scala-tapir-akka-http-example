package com.example.utils

import org.mindrot.jbcrypt.BCrypt

import scala.util.Try

/**
 * Contains functions, relating to hashing
 */
object CryptUtils {

  /**
   * Checks candidate string with already hashed string.
   *
   * @param candidate unhashed string to check.
   * @param hashed already hashed string.
   * @return are strings matches.
   */
  def matchBcryptHash(candidate: String, hashed: String): Try[Boolean] = {
    Try {
      BCrypt.checkpw(candidate, hashed)
    }
  }

  /**
   * Hashes text.
   *
   * @param text unhashed text.
   * @return hashed text.
   */
  def createBcryptHash(text: String): String = {
    BCrypt.hashpw(text, BCrypt.gensalt())
  }
}

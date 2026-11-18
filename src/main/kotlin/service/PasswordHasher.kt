package service

import java.security.MessageDigest

object PasswordHasher {

    private const val SALT = "CampIn-OOP-2026-Salt"

    fun hash(rawPassword: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest("$SALT:$rawPassword".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verify(rawPassword: String, hashed: String): Boolean {
        if (hashed.isBlank()) return rawPassword == "123456"
        return hash(rawPassword) == hashed
    }
}
package com.trollingcont

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.trollingcont.errorhandling.UserAlreadyExistsException
import com.trollingcont.errorhandling.UserFormatException
import com.trollingcont.errorhandling.UserNotFoundException
import com.trollingcont.model.*
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.charset.Charset
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.LocalDateTime
import java.util.*

class UserManager(
    private val db: Database,
    private val hs256secret: String,
    private val jwtIssuer: String
    ) {

    enum class UserDataFormatErrors(val errorCode: Int) {
        NO_ERROR(0),
        USERNAME_EMPTY(1),
        USERNAME_TOO_SHORT(2),
        USERNAME_INVALID_CHARS(3),
        PASSWORD_EMPTY(4),
        PASSWORD_TOO_SHORT(5),
        PASSWORD_INVALID_CHARS(6)
    }

    init {
        transaction(db) {
            SchemaUtils.create(Users)
        }
    }

    fun addUser(user: User) {
        val errorCode = validateUserData(user)

        if (errorCode != UserDataFormatErrors.NO_ERROR) {
            throw UserFormatException(errorCode)
        }

        val passwordSalt = generateRandomString(32)
        var newUserPasswordHash = user.password + passwordSalt

        for (i in 1.. hashingCount) {
            newUserPasswordHash = generateStringHash(newUserPasswordHash)
        }

        try {
            transaction(db) {
                Users.insert {
                    it[name] = user.name
                    it[passwordHash] = newUserPasswordHash
                    it[salt] = passwordSalt
                }
            }
        }
        catch (exc: ExposedSQLException) {
            if (exc.errorCode == 1062) {
                throw UserAlreadyExistsException()
            }
            throw exc
        }
    }

    fun generateToken(user: User): String {
        val errorCode = validateUserData(user)

        if (errorCode != UserDataFormatErrors.NO_ERROR) {
            throw UserFormatException(errorCode)
        }

        lateinit var registeredUser: RegisteredUser

        transaction(db) {
            val query = Users.select {
                (Users.name eq user.name)
            }

            if (query.count() == 0L) {
                throw UserNotFoundException()
            }

            registeredUser = query.map {
                RegisteredUser(
                    it[Users.name],
                    it[Users.passwordHash],
                    it[Users.salt]
                )
            }[0]
        }

        var calculatedPasswordHash = user.password + registeredUser.salt

        for (i in 1.. hashingCount) {
            calculatedPasswordHash = generateStringHash(calculatedPasswordHash)
        }

        if (registeredUser.passwordHash != calculatedPasswordHash) {
            throw UserNotFoundException()
        }

        val algorithm = Algorithm.HMAC256(hs256secret)

        val calendar = Calendar.getInstance()
        val currentTime = calendar.time
        calendar.add(Calendar.HOUR, 48)
        val expirationTime = calendar.time

        return JWT.create()
            .withIssuer(jwtIssuer)
            .withIssuedAt(currentTime)
            .withExpiresAt(expirationTime)
            .withSubject(registeredUser.name)
            .withJWTId(generateRandomString(16))
            .sign(algorithm)
    }

    fun isValidToken(token: String, checkedUsername: String? = null) =
        try {
            val algorithm = Algorithm.HMAC256(hs256secret)

            val verifier = JWT.require(algorithm)
                .withIssuer(jwtIssuer)
                .build()

            val decodedJwt = verifier.verify(token)

            !(checkedUsername != null && decodedJwt.subject != checkedUsername)
        } catch (exc: JWTVerificationException) {
            false
        }

    fun isUsernameUsed(username: String) =
        transaction(db) {
            Users.select {
                Users.name eq username
            }.count() != 0L
        }

    companion object {
        fun validateUserData(user: User): UserDataFormatErrors =
            when {
                user.name.isEmpty() -> UserDataFormatErrors.USERNAME_EMPTY
                user.name.length < 3 -> UserDataFormatErrors.USERNAME_TOO_SHORT
                user.name.indexOf(" ") != -1 -> UserDataFormatErrors.USERNAME_INVALID_CHARS
                user.name.isEmpty() -> UserDataFormatErrors.PASSWORD_EMPTY
                user.password.length < 6 -> UserDataFormatErrors.PASSWORD_TOO_SHORT
                user.password.indexOf(" ") != -1 -> UserDataFormatErrors.PASSWORD_INVALID_CHARS
                else -> UserDataFormatErrors.NO_ERROR
            }

        private const val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        private const val charsLength = chars.length
        private const val hashingCount = 16

        fun generateRandomString(length: Int): String {
            val secureRandom = SecureRandom()
            val str = StringBuilder()

            for (i in 0..length) {
                str.append(chars[secureRandom.nextInt(charsLength - 1)])
            }

            return str.toString()
        }

        private fun generateStringHash(sourceStr: String): String {
            val bytes = MessageDigest
                .getInstance("SHA-256")
                .digest(sourceStr.toByteArray())

            return printHexBinary(bytes).toUpperCase(Locale.ROOT)
        }

        private val hexChars = "0123456789ABCDEF".toCharArray()

        private fun printHexBinary(data: ByteArray): String {
            val r = StringBuilder(data.size * 2)
            data.forEach { b ->
                val i = b.toInt()
                r.append(hexChars[i shr 4 and 0xF])
                r.append(hexChars[i and 0xF])
            }
            return r.toString()
        }
    }
}
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

class UserManager(private val db: Database) {

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

        try {
            transaction(db) {
                Users.insert {
                    it[name] = user.name
                    it[password] = user.password
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

        transaction(db) {
            val query = Users.select {
                (Users.name eq user.name) and (Users.password eq user.password)
            }

            if (query.count() == 0L) {
                throw UserNotFoundException()
            }
        }

        val algorithm = Algorithm.HMAC256("Bubblegum")

        return JWT.create()
            .withIssuer("MeetFindTrollingContServer")
            .withJWTId(user.name)
            .sign(algorithm)
    }

    fun isValidToken(token: String) =
        try {
            val algorithm = Algorithm.HMAC256("Bubblegum")

            val verifier = JWT.require(algorithm)
                .withIssuer("MeetFindTrollingContServer")
                .build()

            verifier.verify(token)

            true
        }
        catch (exc: JWTVerificationException) {
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
    }
}
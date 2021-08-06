package com.trollingcont

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.squareup.moshi.Json
import com.trollingcont.errorhandling.*
import com.trollingcont.model.Meet
import com.trollingcont.model.MeetCreationData
import com.trollingcont.model.ServerConfig
import com.trollingcont.model.User
import org.apache.log4j.PropertyConfigurator
import org.http4k.core.*
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.CREATED
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.filter.DebuggingFilters.PrintRequest
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.jetbrains.exposed.sql.Database
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Paths
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.system.exitProcess


const val configFileName = "ServerConfig.json"
const val loggerPropertiesFileName = "log4j.properties"

fun main() {
    PropertyConfigurator.configure(loggerPropertiesFileName)
    val gson = Gson()

    val currentDirectory = Paths.get("").toAbsolutePath().toString()

    println("Current directory: '$currentDirectory'")

    lateinit var serverConfig: ServerConfig

    try {
        serverConfig = gson.fromJson(
            File(configFileName).readText(Charsets.UTF_8),
            ServerConfig::class.java
        )
    }
    catch (exc: Exception) {
        println("Unable to start server: failed to read configuration file $configFileName: $exc")
        exitProcess(-1)
    }

    println("Configuration file $configFileName loaded")
    println("Database address: ${serverConfig.databaseConnectionParams.host}:" +
            "${serverConfig.databaseConnectionParams.port}, " +
            "database name: ${serverConfig.databaseConnectionParams.dbName}, " +
            "user: ${serverConfig.databaseConnectionParams.user}")

    lateinit var database: Database

    try {
        val connectionParams = serverConfig.databaseConnectionParams

        database = Database.connect(
            "jdbc:mysql://${connectionParams.host}:${connectionParams.port}/${connectionParams.dbName}",
            driver = "com.mysql.jdbc.Driver",
            user = connectionParams.user,
            password = connectionParams.password
        )
    }
    catch (exc: Exception) {
        println("Unable to start server: failed to connect to database: $exc")
        exitProcess(-1)
    }

    lateinit var databaseManager: DatabaseManager

    try {
        databaseManager = DatabaseManager(
            database,
            serverConfig.hs256secret,
            serverConfig.jwtIssuer
        )
    }
    catch (exc: Exception) {
        println("Unable to start server: internal error: $exc")
    }

    println("Successfully connected to database")

    val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    val app: HttpHandler = routes(
        "/register" bind Method.POST to {
                req: Request ->
            val requestBody = req.bodyString()

            try {
                val jsonObject = JsonParser.parseString(requestBody).asJsonObject
                val user = User(jsonObject.get("name").asString, jsonObject.get("password").asString)

                try {
                    databaseManager.addUser(user)
                    println("[REQUEST HANDLER] POST /register :: New user '${user.name}' successfully added")
                    Response(CREATED)
                }
                catch (ufe: UserFormatException) {
                    println("[REQUEST HANDLER] POST /register :: User data is not valid, error code ${ufe.code().ordinal}")
                    Response(BAD_REQUEST).body(ufe.code().ordinal.toString())
                }
                catch (uae: UserAlreadyExistsException) {
                    println("[REQUEST HANDLER] POST /register :: attempt to add user with already existing name '${user.name}'")
                    Response(BAD_REQUEST)
                }
                catch (exc: Exception) {
                    println("[REQUEST HANDLER][SERVER ERROR] POST /register :: exception ${exc.printStackTrace()}")
                    Response(INTERNAL_SERVER_ERROR)
                }
            }
            catch (exc: Exception) {
                println("[REQUEST HANDLER] POST /register :: Failed to parse JSON data.\n$exc")
                Response(BAD_REQUEST).body(exc.toString())
            }
        },

        "/login" bind Method.POST to {
                req: Request ->
            val requestBody = req.bodyString()

            try {
                val jsonObject = JsonParser.parseString(requestBody).asJsonObject
                val user = User(jsonObject.get("name").asString, jsonObject.get("password").asString)

                try {
                    val newToken = databaseManager.generateToken(user)
                    println("[REQUEST HANDLER] POST /login :: User '${user.name}' - new JWT successfully generated")
                    Response(CREATED).body(newToken)
                }
                catch (ufe: UserFormatException) {
                    println("[REQUEST HANDLER] POST /login :: User data is not valid, error code ${ufe.code().ordinal}")
                    Response(BAD_REQUEST)
                }
                catch (unf: UserNotFoundException) {
                    println("[REQUEST HANDLER] POST /login :: Attempt lo get token with wrong username/password. Username: '${user.name}'")
                    Response(BAD_REQUEST)
                }
                catch (exc: Exception) {
                    println("[REQUEST HANDLER][SERVER ERROR] POST /login :: exception ${exc.printStackTrace()}")
                    Response(INTERNAL_SERVER_ERROR)
                }
            }
            catch (exc: Exception) {
                println("[REQUEST HANDLER] POST /login :: Failed to parse JSON data.\n$exc")
                Response(BAD_REQUEST).body(exc.toString())
            }
        },

        "/test_req" bind Method.GET to {
                req: Request ->

            try {
                val token = req.header("Authorization")

                if (token == null || !databaseManager.isValidToken(token)) {
                    throw UnauthorizedAccessException()
                }

                println("[REQUEST HANDLER] GET /test_req :: Success, token is valid")
                Response(OK).body("Successfully authorized")
            }
            catch (ua: UnauthorizedAccessException) {
                println("[REQUEST HANDLER] GET /test_req :: Attempt to access without valid token")
                Response(UNAUTHORIZED).body("Not authorized: token is not valid or missing")
            }
        },

        "/create_meet" bind Method.POST to {
                req: Request ->

            val requestBody = req.bodyString()

            try {
                val token = req.header("Authorization") ?: throw UnauthorizedAccessException()

                val jsonObject = JsonParser.parseString(requestBody).asJsonObject

                val meetCreationData = MeetCreationData(
                    jsonObject.get("name").asString,
                    jsonObject.get("description").asString,
                    jsonObject.get("latitude").asDouble,
                    jsonObject.get("longitude").asDouble,
                    LocalDateTime.parse(jsonObject.get("time").asString, dateTimeFormatter),
                    jsonObject.get("creatorUsername").asString
                )

                if (!databaseManager.isValidToken(token, meetCreationData.creatorUsername)) {
                    throw UnauthorizedAccessException()
                }

                try {
                    val createdMeet = databaseManager.addMeet(meetCreationData)
                    Response(CREATED).body(createdMeet.toJson(dateTimeFormatter))
                }
                catch (mcd: MeetCreationDataException) {
                    println("[REQUEST HANDLER] POST /create_meet :: Meet creation data is not valid. '$requestBody'," +
                            " error code ${mcd.code().ordinal}")
                    Response(BAD_REQUEST).body(mcd.code().ordinal.toString())
                }
                catch (exc: Exception) {
                    println("[REQUEST HANDLER][SERVER ERROR] POST /create_meet :: Exception $exc")
                    Response(INTERNAL_SERVER_ERROR)
                }
            }
            catch (ua: UnauthorizedAccessException) {
                println("[REQUEST HANDLER] POST /create_meet :: Attempt to access without valid token")
                Response(UNAUTHORIZED)
            }
            catch (exc: Exception) {
                println("[REQUEST HANDLER] POST /create_meet :: Failed to parse JSON data. Body: '$requestBody' \n$exc")
                Response(BAD_REQUEST)
            }
        }
    )

    val printingApp: HttpHandler = PrintRequest().then(app)
    lateinit var server: Http4kServer

    try {
        server = printingApp.asServer(SunHttp(serverConfig.serverPort)).start()
    }
    catch (exc: Exception) {
        println("Failed to start server on ${serverConfig.serverPort}: $exc")
        exitProcess(-1)
    }

    println("Server started on ${server.port()}, ready to accept connections")
}
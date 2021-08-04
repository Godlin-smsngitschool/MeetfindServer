package com.trollingcont.model

data class ServerConfig(
    val databaseConnectionParams: DatabaseConnectionParams,
    val serverPort: Int,
    val hs256secret: String,
    val jwtIssuer: String
)

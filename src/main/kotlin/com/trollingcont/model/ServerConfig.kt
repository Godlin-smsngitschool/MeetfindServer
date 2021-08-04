package com.trollingcont.model

data class ServerConfig(
    val databaseConnectionParams: DatabaseConnectionParams,
    val serverPort: Int
)

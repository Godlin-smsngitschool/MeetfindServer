package com.trollingcont.model

data class DatabaseConnectionParams(
    val host: String,
    val port: Int,
    val dbName: String,
    val user: String,
    val password: String
)

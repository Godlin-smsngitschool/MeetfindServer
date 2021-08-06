package com.trollingcont.model

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class Meet(
    val name: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val time: LocalDateTime,
    val creatorUsername: String,
    val id: Int,
    val timeCreated: LocalDateTime
) {
    fun toJson(dateTimeFormatter: DateTimeFormatter): String =
        "{\"name\":\"$name\"," +
                "\"description\":\"$description\"," +
                "\"latitude\":$latitude," +
                "\"longitude\":$longitude," +
                "\"time\":\"${time.format(dateTimeFormatter)}\"," +
                "\"creatorUsername\":$creatorUsername\"," +
                "\"id\":$id," +
                "\"timeCreated\":\"${timeCreated.format(dateTimeFormatter)}\"}"
}

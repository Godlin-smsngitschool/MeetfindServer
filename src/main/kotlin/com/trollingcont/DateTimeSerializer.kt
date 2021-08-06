package com.trollingcont

import com.google.gson.*
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DateTimeSerializer(private val dateTimeFormatter: DateTimeFormatter) : JsonSerializer<LocalDateTime> {

    override fun serialize(src: LocalDateTime?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement =
        JsonPrimitive(src!!.format(dateTimeFormatter))
}
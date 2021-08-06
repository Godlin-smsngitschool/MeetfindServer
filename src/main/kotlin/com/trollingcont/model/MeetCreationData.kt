package com.trollingcont.model

import java.time.LocalDateTime

data class MeetCreationData(
    val name: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val time: LocalDateTime,
    val creatorUsername: String
)

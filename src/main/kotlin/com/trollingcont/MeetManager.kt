package com.trollingcont

import com.trollingcont.errorhandling.MeetCreationDataException
import com.trollingcont.model.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class MeetManager(private val db: Database) {

    enum class MeetCreationDataErrors(val errorCode: Int) {
        NO_ERROR(0),
        NAME_EMPTY(1),
        NAME_TOO_SHORT(2),
        TIME_EMPTY(3),
        WRONG_TIME_FORMAT(4)
    }

    init {
        transaction(db) {
            SchemaUtils.create(Meets)
            SchemaUtils.create(MeetParticipants)
        }
    }

    fun addMeet(meetCreationData: MeetCreationData) {
        val errorCode = validateMeetCreationData(meetCreationData)

        if (errorCode != MeetCreationDataErrors.NO_ERROR) {
            throw MeetCreationDataException(errorCode)
        }

        lateinit var meetTimeScheduled: LocalDateTime

        try {
            meetTimeScheduled = LocalDateTime.parse(meetCreationData.time, dateTimeFormatter)
        }
        catch (dtp: DateTimeParseException) {
            throw MeetCreationDataException(MeetCreationDataErrors.WRONG_TIME_FORMAT)
        }

        transaction(db) {
            Meets.insert {
                it[name] = meetCreationData.name
                it[description] = meetCreationData.description
                it[latitude] = meetCreationData.latitude
                it[longitude] = meetCreationData.longitude
                it[timeScheduled] = meetTimeScheduled
                it[timeCreated] = LocalDateTime.now()
                it[creator] = meetCreationData.creatorUsername
            }
        }
    }

    fun removeMeet(meetId: Int) {

    }

    fun addMeetParticipant(meetId: Int, username: String) {

    }

    fun removeMeetParticipant(meetId: Int, username: String) {
    }

    companion object {
        private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

        private fun validateMeetCreationData(meetCreationData: MeetCreationData) =
            when {
                meetCreationData.name.isEmpty() || meetCreationData.name.isBlank() -> MeetCreationDataErrors.NAME_EMPTY
                meetCreationData.name.length < 3 -> MeetCreationDataErrors.NAME_TOO_SHORT
                meetCreationData.name.isEmpty() || meetCreationData.name.isBlank() -> MeetCreationDataErrors.TIME_EMPTY
                else -> MeetCreationDataErrors.NO_ERROR
            }
        }
}
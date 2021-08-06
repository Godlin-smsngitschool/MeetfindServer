package com.trollingcont

import com.trollingcont.errorhandling.MeetCreationDataException
import com.trollingcont.errorhandling.MeetCreationException
import com.trollingcont.model.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class MeetManager(private val db: Database) {

    enum class MeetCreationDataErrors(val errorCode: Int) {
        NO_ERROR(0),
        NAME_EMPTY(1),
        NAME_TOO_SHORT(2),
        TIME_EMPTY(3)
    }

    init {
        transaction(db) {
            SchemaUtils.create(Meets)
            SchemaUtils.create(MeetParticipants)
        }
    }

    fun addMeet(meetCreationData: MeetCreationData): Meet {
        val errorCode = validateMeetCreationData(meetCreationData)

        if (errorCode != MeetCreationDataErrors.NO_ERROR) {
            throw MeetCreationDataException(errorCode)
        }

        lateinit var meetResult: Meet

        transaction(db) {
            val result = Meets.insert {
                it[name] = meetCreationData.name
                it[description] = meetCreationData.description
                it[latitude] = meetCreationData.latitude
                it[longitude] = meetCreationData.longitude
                it[timeScheduled] = meetCreationData.time
                it[timeCreated] = LocalDateTime.now()
                it[creator] = meetCreationData.creatorUsername
            }.resultedValues

            if (result != null) {
                meetResult = result.map {
                    Meet(
                        it[Meets.name],
                        it[Meets.description],
                        it[Meets.latitude],
                        it[Meets.longitude],
                        it[Meets.timeScheduled],
                        it[Meets.creator],
                        it[Meets.id],
                        it[Meets.timeCreated]
                    )
                }[0]
            }
            else {
                throw MeetCreationException()
            }
        }

        return meetResult
    }

    fun getMeetsList(): List<Meet> =
        transaction(db) {
            Meets.selectAll()
                .map {
                    Meet(
                        it[Meets.name],
                        it[Meets.description],
                        it[Meets.latitude],
                        it[Meets.longitude],
                        it[Meets.timeScheduled],
                        it[Meets.creator],
                        it[Meets.id],
                        it[Meets.timeCreated]
                    )}
        }

    fun removeMeet(meetId: Int) {

    }

    fun addMeetParticipant(meetId: Int, username: String) {

    }

    fun removeMeetParticipant(meetId: Int, username: String) {
    }

    companion object {
        private fun validateMeetCreationData(meetCreationData: MeetCreationData) =
            when {
                meetCreationData.name.isEmpty() || meetCreationData.name.isBlank() -> MeetCreationDataErrors.NAME_EMPTY
                meetCreationData.name.length < 3 -> MeetCreationDataErrors.NAME_TOO_SHORT
                meetCreationData.name.isEmpty() || meetCreationData.name.isBlank() -> MeetCreationDataErrors.TIME_EMPTY
                else -> MeetCreationDataErrors.NO_ERROR
            }
        }
}
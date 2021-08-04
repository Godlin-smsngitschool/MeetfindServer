package com.trollingcont

import com.trollingcont.model.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

class MeetManager(db: Database) {

    init {
        transaction(db) {
            SchemaUtils.create(Meets)
            SchemaUtils.create(MeetParticipants)
        }
    }

    fun addMeet(meetCreationData: MeetCreationData) {

    }

    fun removeMeet(meetId: Int) {

    }

    fun addMeetParticipant(meetId: Int, username: String) {

    }

    fun removeMeetParticipant(meetId: Int, username: String) {

    }
}
package com.trollingcont

import com.trollingcont.model.*
import org.jetbrains.exposed.sql.Database
import java.lang.Exception

class DatabaseManager(db: Database) {

    private val userManager = UserManager(db)
    private val meetManager = MeetManager(db)

    fun addUser(user: User) {
        userManager.addUser(user)
    }

    fun generateToken(user: User) =
        userManager.generateToken(user)

    fun isValidToken(token: String) =
        userManager.isValidToken(token)

    fun addMeet(meetCreationData: MeetCreationData) {

    }

    fun addMeetParticipant(meetId: Int, participantUsername: String) {

    }

    fun removeMeetParticipant(meetId: Int, participantUsername: String) {

    }

    fun removeMeet(meetId: Int) {

    }
}
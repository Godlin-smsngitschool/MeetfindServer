package com.trollingcont

import com.trollingcont.model.*
import org.jetbrains.exposed.sql.Database
import java.lang.Exception

class DatabaseManager(
    db: Database,
    hs256secret: String,
    jwtIssuer: String
) {

    private val userManager = UserManager(db, hs256secret, jwtIssuer)
    private val meetManager = MeetManager(db)

    fun addUser(user: User) {
        userManager.addUser(user)
    }

    fun generateToken(user: User) =
        userManager.generateToken(user)

    fun isValidToken(token: String) =
        userManager.isValidToken(token)

    fun isUsernameUsed(username: String) =
        userManager.isUsernameUsed(username)

    fun addMeet(meetCreationData: MeetCreationData) {
        meetManager.addMeet(meetCreationData)
    }

    fun addMeetParticipant(meetId: Int, participantUsername: String) {

    }

    fun removeMeetParticipant(meetId: Int, participantUsername: String) {

    }

    fun removeMeet(meetId: Int) {

    }
}
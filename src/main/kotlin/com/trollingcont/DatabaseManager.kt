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

    fun isValidToken(token: String, checkedUsername: String? = null) =
        userManager.isValidToken(token, checkedUsername)

    fun isUsernameUsed(username: String) =
        userManager.isUsernameUsed(username)

    fun addMeet(meetCreationData: MeetCreationData): Meet =
        meetManager.addMeet(meetCreationData)

    fun getMeetsList(): List<Meet> =
        meetManager.getMeetsList()

    fun getMeetById(meetId: Int) =
        meetManager.getMeetById(meetId)

    fun addMeetParticipant(meetId: Int, participantUsername: String) {
        meetManager.addMeetParticipant(meetId, participantUsername)
    }

    fun removeMeetParticipant(meetId: Int, participantUsername: String) {
        meetManager.removeMeetParticipant(meetId, participantUsername)
    }

    fun removeMeet(meetId: Int) {

    }
}
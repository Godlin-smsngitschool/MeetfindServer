package com.trollingcont.model

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.`java-time`.datetime

object Users : Table() {
    val id = integer("id").autoIncrement()
    val name = text("name").uniqueIndex()
    val passwordHash = text("passwordHash")
    val salt = text("salt")
    override val primaryKey = PrimaryKey(id)
}

object Meets : Table() {
    val id = integer("id").autoIncrement()
    val description = text("description")
    val creator = text("creator")
    val name = text("name")
    val latitude = double("latitude")
    val longitude = double("longitude")
    val timeScheduled = datetime("timeScheduled")
    val timeCreated = datetime("timeCreated")
    override val primaryKey = PrimaryKey(id)
}

object MeetParticipants : Table() {
    val meetId = integer("meetId")
    val user = text("user")
}
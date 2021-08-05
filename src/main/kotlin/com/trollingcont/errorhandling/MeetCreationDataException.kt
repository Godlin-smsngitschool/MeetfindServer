package com.trollingcont.errorhandling

import com.trollingcont.MeetManager

class MeetCreationDataException(private val errorCode: MeetManager.MeetCreationDataErrors) : Throwable() {
    fun code() = errorCode
}
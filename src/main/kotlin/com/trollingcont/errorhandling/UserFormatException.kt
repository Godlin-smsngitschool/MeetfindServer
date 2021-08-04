package com.trollingcont.errorhandling

import com.trollingcont.UserManager

class UserFormatException(private val code: UserManager.UserDataFormatErrors) : Throwable() {
    fun code() = code
}
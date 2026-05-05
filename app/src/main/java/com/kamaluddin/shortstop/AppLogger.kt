package com.kamaluddin.shortstop

import android.util.Log

object AppLogger {
    private fun String.sanitize() = replace("\n", "").replace("\r", "")

    fun d(tag: String, msg: String) {
        if (BuildConfig.DEBUG) Log.d(tag.sanitize(), msg.sanitize())
    }

    fun w(tag: String, msg: String) {
        Log.w(tag.sanitize(), msg.sanitize())
    }

    fun e(tag: String, msg: String, t: Throwable? = null) {
        if (t != null) Log.e(tag.sanitize(), msg.sanitize(), t)
        else Log.e(tag.sanitize(), msg.sanitize())
    }
}

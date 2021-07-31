package com.tuya.smart.elapse

import android.util.Log

interface ElapseLogger {
    fun i(tag: String = "elapse", msg: String)
    fun d(tag: String = "elapse", msg: String)
    fun e(tag: String = "elapse", msg: String, e: Throwable? = null)

    object Default: ElapseLogger {
        override fun i(tag: String, msg: String) {
            Log.i(tag, msg)
        }

        override fun d(tag: String, msg: String) {
            Log.d(tag, msg)
        }

        override fun e(tag: String, msg: String, e: Throwable?) {
            Log.e(tag, msg, e)
        }
    }


    enum class LogLevel {
        Slow, Error, Debug, All;
    }
}
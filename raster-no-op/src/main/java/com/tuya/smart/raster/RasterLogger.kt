package com.tuya.smart.raster

import android.util.Log

interface RasterLogger {
    fun i(tag: String = "raster", msg: String)
    fun d(tag: String = "raster", msg: String)
    fun e(tag: String = "raster", msg: String, e: Throwable? = null)

    object Default: RasterLogger {
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
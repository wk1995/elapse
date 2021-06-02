package com.tuya.smart.raster

import android.util.Log

interface RasterLogger {
    fun i(tag: String = "raster", msg: String)
    fun d(tag: String = "raster", msg: String)
    fun e(tag: String = "raster", msg: String, e: Throwable? = null)
    fun s(tag: String = "raster", msg: String)

    object Default: RasterLogger {
        override fun i(tag: String, msg: String) {
            if (Raster.logLevel >= LogLevel.All) {
                Log.i(tag, msg)
            }
        }

        override fun d(tag: String, msg: String) {
            if (Raster.logLevel >= LogLevel.Debug) {
                Log.d(tag, msg)
            }
        }

        override fun e(tag: String, msg: String, e: Throwable?) {
            if (Raster.logLevel >= LogLevel.Error) {
                Log.e(tag, msg, e)
            }
        }

        override fun s(tag: String, msg: String) {
            if (Raster.logLevel >= LogLevel.Slow) {
                Log.e(tag, msg)
            }
        }
    }


    enum class LogLevel {
        Slow, Error, Debug, All;
    }
}
package com.tuya.smart.raster

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Looper
import java.text.SimpleDateFormat
import java.util.*

internal object Util {

    private val durationFormat = SimpleDateFormat("ss.SSS", Locale.CHINA)

    private const val DURATION_ONE_SECOND = 1000

    private const val DURATION_ONE_MINUTE = 60 * DURATION_ONE_SECOND

    private const val DURATION_ONE_HOUR = 60 * DURATION_ONE_MINUTE

    private const val DURATION_ONE_DAY = 24 * DURATION_ONE_HOUR

    fun checkInMainThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw IllegalStateException("method call not in main thread!!!")
        }
    }

    /**
     * 判断当前是否是debug模式
     */
    fun debuggable(context: Context): Boolean {
        return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    /**
     *  以Raster初始化为0点开始的时间的相对时间, 可看做是app启动后经历的时间
     *
     *  @param time 取当前的SystemClock.elapsedRealTime()
     */
    fun relativeTime(time: Long): String {
        if (time <= 0) {
            return "$time"
        }
        var final = ""
        var relativeTime = time - Raster.zeroTime
        if (relativeTime > DURATION_ONE_DAY) {
            final += "${ relativeTime / DURATION_ONE_DAY }d "
            relativeTime %= DURATION_ONE_DAY
        }
        if (relativeTime > DURATION_ONE_HOUR) {
            final += "${ relativeTime / DURATION_ONE_HOUR }:"
            relativeTime %= DURATION_ONE_HOUR
        }
        val minute = relativeTime / DURATION_ONE_MINUTE
        final += if (minute < 10) {
            "0${ minute }:"
        } else {
            "${minute}:"
        }
        relativeTime %= DURATION_ONE_MINUTE
        val second = relativeTime / DURATION_ONE_SECOND
        val millis = relativeTime % DURATION_ONE_SECOND
        final += if (second < 10) {
            "0${ second }.${millis}"
        } else {
            "${ second }.${millis}"
        }
        return final
    }

    fun formatTime(time: Long): String {
        return durationFormat.format(time)
    }

}
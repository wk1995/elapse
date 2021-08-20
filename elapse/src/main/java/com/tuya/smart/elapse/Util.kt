package com.tuya.smart.elapse

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Looper
import android.os.Process
import android.text.TextUtils
import com.tuya.smart.elapse.Elapse.context
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
     *  以Elapse初始化为0点开始的时间的相对时间, 可看做是app启动后经历的时间
     *
     *  @param time 取当前的SystemClock.elapsedRealTime()
     */
    fun relativeTime(time: Long): String {
        if (time <= 0) {
            return "$time"
        }
        var final = ""
        var relativeTime = time - Elapse.zeroTime
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

    internal val processName: String
        get() = initProcessName()!!

    private var mProcessName: String? = null

    private fun initProcessName(): String? {
        if (!TextUtils.isEmpty(mProcessName)) {
            return mProcessName
        }
        if (Build.VERSION.SDK_INT >= 28) {
            mProcessName = Application.getProcessName()
        } else {
            try {
                val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val runningApps = am.runningAppProcesses
                if (runningApps != null) {
                    for (processInfo in runningApps) {
                        if (processInfo.pid == Process.myPid()) {
                            mProcessName = processInfo.processName
                        }
                    }
                }
                if (TextUtils.isEmpty(mProcessName)) {
                    mProcessName = context.packageName
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        mProcessName = mProcessName?.replace(":", ".")
        return mProcessName
    }
}
package com.tuya.smart.raster

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import kotlin.math.max

@SuppressLint("StaticFieldLeak")
object Raster {

    internal const val START_FLAG = ">>>>> Dispatching to "

    internal val myPid = Process.myPid()

    const val END_FLAG = "<<<<< Finished to "

    internal var slowThreshold: Long = -1L

    internal var idleThreshold: Long = -1L

    internal var enqueueDelay: Long = -1L

    internal var recordMaxDuration = -1

    internal var timeLineDuration = -1

    internal var maxRecordCount = -1

    internal val zeroTime = SystemClock.uptimeMillis()

    internal lateinit var monitor: RasterMonitor

    internal lateinit var dumper: RasterDumper

    internal lateinit var applicationId: String

    internal var debuggable: Boolean = false

    internal lateinit var logger: RasterLogger

    internal var logLevel = RasterLogger.LogLevel.Slow

    internal lateinit var context: Context

    @JvmStatic @JvmOverloads fun init(context: Context, options: RasterOptions = RasterOptions()) {
        Util.checkInMainThread()
        Raster.context = context.applicationContext
        applicationId = context.packageName
        debuggable = Util.debuggable(context)

        slowThreshold = options.slowThreshold
        idleThreshold = options.idleThreshold
        enqueueDelay = max(slowThreshold * 2, 3000L)
        recordMaxDuration = options.recordMaxDuration
        timeLineDuration = options.timeLineDuration
        logLevel = options.logLevel
        maxRecordCount = timeLineDuration / recordMaxDuration / 3
        logger = options.logger

        dumper = RasterDumper()

        monitor = RasterMonitor()
        monitor.start()
        dumper.start()
        Looper.getMainLooper().setMessageLogging(RasterHandler()) // todo 可能会覆盖已存在的MessageLogging
    }

    @JvmStatic fun dump() {
        monitor.dumpTimeLine()
    }
}
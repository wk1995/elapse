package com.tuya.smart.elapse

import android.os.Process
import android.system.Os
import android.system.OsConstants
import java.lang.reflect.Method

internal object ElapseCpuTimeTracker {

    /** @see android.os.Process.PROC_SPACE_TERM */
    private const val PROC_SPACE_TERM = ' '.toInt()

    /** @see android.os.Process.PROC_OUT_LONG */
    private const val PROC_OUT_LONG = 0x2000

    /** @see android.os.Process.PROC_PARENS */
    private const val PROC_PARENS = 0x200

    private const val PROCESS_STAT_UTIME = 2
    private const val PROCESS_STAT_STIME = 3

    var mJiffyMillis = 1000 / Os.sysconf(OsConstants._SC_CLK_TCK)

    private val PROCESS_STATS_FORMAT = intArrayOf(
        PROC_SPACE_TERM,
        PROC_SPACE_TERM or PROC_PARENS,
        PROC_SPACE_TERM,
        PROC_SPACE_TERM,
        PROC_SPACE_TERM,
        PROC_SPACE_TERM,
        PROC_SPACE_TERM,
        PROC_SPACE_TERM,
        PROC_SPACE_TERM,
        PROC_SPACE_TERM or PROC_OUT_LONG,  // 10: minor faults
        PROC_SPACE_TERM,
        PROC_SPACE_TERM or PROC_OUT_LONG,  // 12: major faults
        PROC_SPACE_TERM,
        PROC_SPACE_TERM or PROC_OUT_LONG,  // 14: utime
        PROC_SPACE_TERM or PROC_OUT_LONG   // 15: stime
    )

    private var readProcFileMethod: Method? = null

    /** Stores user time and system time in jiffies.  Used for
     * public API to retrieve CPU use for a process.  Must lock while in use.  */
    private val mSinglePidStatsData = LongArray(4)


    init {
        readProcFileMethod = Process::class.java.getMethod(
            "readProcFile",
            String::class.java, IntArray::class.java,
            Array<String>::class.java, LongArray::class.java,
            FloatArray::class.java
        )
        readProcFileMethod!!.isAccessible = true
    }

    fun getCpuTimeForTid(pid:Int, tid: Int): Long {
        synchronized(mSinglePidStatsData) {
            val statsData = mSinglePidStatsData
            readProcFileMethod?.invoke(
                null,
                "/proc/$pid/task/$tid/stat",
                PROCESS_STATS_FORMAT,
                null, statsData, null
            )
            return statsData[PROCESS_STAT_UTIME] + statsData[PROCESS_STAT_STIME] * mJiffyMillis
        }

    }

}
package com.tuya.smart.raster

import java.util.concurrent.ConcurrentLinkedDeque

internal class RasterTimeLine: Iterable<RasterRecord> {

    private val mDequeue = ConcurrentLinkedDeque<RasterRecord>()

    private var mCount = 0

    fun addRecord(r: RasterRecord) {
        mDequeue.offer(r)
//        if (r.name != "IDLE") {
            if (Raster.logLevel >= RasterLogger.LogLevel.Debug) {
                Raster.logger.d(msg = r.toString())
            }
//        }
        mCount ++
        val edge = mDequeue.last.end!! - Raster.timeLineDuration
        if (edge < 0) {
            return
        }
        do {
            val first = mDequeue.peekFirst()
//            val time = mDequeue.last.end!! - mDequeue.first.start
//            println("$time -> $edge")
            val brk = first?.run {
                if (start < edge) {
//                    println("on Evict: $this")
                    mDequeue.pollFirst()
                    mCount --
                    onEvict(this)
                    return@run false
                }
                return@run true
            }
            if (brk == null || brk) {
                break
            }
        } while (true)
    }


    private fun onEvict(t: RasterRecord) {
        t.recycle()
    }

    fun getCount(): Int {
        return mCount
    }

    override fun iterator(): Iterator<RasterRecord> {
        return mDequeue.iterator()
    }
}
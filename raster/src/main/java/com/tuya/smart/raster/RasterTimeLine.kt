package com.tuya.smart.raster

import java.util.concurrent.ConcurrentLinkedDeque

internal class RasterTimeLine: Iterable<RasterRecord> {

    private val mList = ConcurrentLinkedDeque<RasterRecord>()

    private var mCount = 0

    fun addRecord(r: RasterRecord) {
        mList.offer(r)
//        if (r.name != "IDLE") {
            if (Raster.logLevel >= RasterLogger.LogLevel.Debug) {
                Raster.logger.d(msg = r.toString())
            }
//        }
        mCount ++
        val edge = mList.first.start - Raster.timeLineDuration
        if (edge < 0) {
            return
        }
        do {
            val l = mList.peek()
            val brk = l?.run {
                if (end!! < edge) {
                    mList.poll()
                    mCount --
                    onEvict(l)
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
        return mList.iterator()
    }
}
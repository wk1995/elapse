package com.tuya.smart.raster

import java.util.concurrent.ConcurrentLinkedQueue

internal class RasterTimeLine: Iterable<RasterRecord> {

    private val mList = ConcurrentLinkedQueue<RasterRecord>()

    private var mCount = 0

    fun addRecord(r: RasterRecord) {
        mList.offer(r)
        if (r.name != "IDLE") {
            Raster.logger.d(msg = r.toString())
        }
        mCount ++
        val edge = r.start - Raster.timeLineDuration
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
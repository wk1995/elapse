package com.tuya.smart.elapse

import java.util.concurrent.ConcurrentLinkedDeque

internal class ElapseTimeLine: Iterable<ElapseRecord> {

    private val mDequeue = ConcurrentLinkedDeque<ElapseRecord>()

    private var mCount = 0

    var runningRecord: ElapseRecord? = null

    var anrTimestamp: Long = -1

    fun addRecord(r: ElapseRecord) {
        runningRecord?.apply {
            if (r.start == start) {
                end = r.end
            }
        }
        mDequeue.offer(r)
//        if (r.name != "IDLE") {
            if (Elapse.logLevel >= ElapseLogger.LogLevel.Debug) {
                Elapse.logger.d(msg = r.toString())
            }
//        }
        mCount ++
        val edge = mDequeue.last.end!! - Elapse.timeLineDuration
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


    private fun onEvict(t: ElapseRecord) {
        t.recycle()
    }

    fun getCount(): Int {
        return mCount
    }

    override fun iterator(): Iterator<ElapseRecord> {
        return mDequeue.iterator()
    }
}
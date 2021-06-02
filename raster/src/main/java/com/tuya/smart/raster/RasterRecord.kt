package com.tuya.smart.raster

internal data class RasterRecord private constructor(var name: String, var count:Int = 0, var start:Long, var end:Long? = null, var cpuTime:Long? = null, var handler: String = "", var callback: String = "", var what: String = "", var stackTrace: Array<StackTraceElement>? = null) {

    companion object {

        private val recordPool = mutableListOf<RasterRecord>()

        fun obtain(name: String, start: Long, end:Long? = null, count: Int = 0, cpuTime: Long? = null): RasterRecord {
            val r = recordPool.removeFirstOrNull()?: RasterRecord(name, start = start)
            r.name = name
            r.start = start
            r.end = end
            r.count = count
            r.cpuTime = cpuTime
            return r
        }
    }

    override fun toString(): String {
        return "RasterRecord(name=$name, count=$count, start=${Util.relativeTime(start)}, end=${
            Util.relativeTime(
                end!!
            )
        }, delta=${deltaTime()} cpuTime=$cpuTime, stackTrace=${stackTrace})"
    }

    fun deltaTime(): Long {
        return end!! - start
    }

    fun recycle() {
        if (recordPool.size < Raster.maxRecordCount) {
            name = ""
            start = -1L
            end = null
            count = 0
            cpuTime = null
            handler = ""
            callback = ""
            stackTrace = null
            what = ""
            recordPool.add(this)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RasterRecord

        if (start != other.start) return false

        return true
    }

    override fun hashCode(): Int {
        return start.hashCode()
    }

}
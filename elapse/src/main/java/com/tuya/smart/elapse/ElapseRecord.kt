package com.tuya.smart.elapse

internal data class ElapseRecord private constructor(var name: String, var count:Int = 0, var start:Long, var end:Long? = null, var cpuTime:Long? = null, var handler: String = "", var callback: String = "", var what: String = "", var stackTrace: Array<StackTraceElement>? = null) {

    var date:Long = System.currentTimeMillis()

    companion object {

        private val recordPool = mutableListOf<ElapseRecord>()

        fun obtain(name: String, start: Long, end:Long? = null, count: Int = 0, cpuTime: Long? = null): ElapseRecord {
            val r = recordPool.removeFirstOrNull()?: ElapseRecord(name, start = start)
            r.name = name
            r.start = start
            r.end = end
            r.count = count
            r.cpuTime = cpuTime
            r.date = System.currentTimeMillis()
            return r
        }
    }

    override fun toString(): String {
        return "${Elapse.dumper.dateTimeFormat.format(date)}: (name=$name, count=$count, start=${Util.relativeTime(start)}, end=${
            Util.relativeTime(
                end ?: -1L
            )
        }, delta=${deltaTime()} cpuTime=$cpuTime, stackTrace=${stackTrace})"
    }

    fun deltaTime(): Long {
        return (end ?: -1) - start
    }

    fun recycle() {
        if (recordPool.size < Elapse.maxRecordCount) {
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

        other as ElapseRecord

        if (start != other.start) return false

        return true
    }

    override fun hashCode(): Int {
        return start.hashCode()
    }

}
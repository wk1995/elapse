package com.tuya.smart.elapse

data class ElapseOptions(@JvmField var slowThreshold: Long = 100L,    // 单条消息超过slowThreshold时，被认为是一条慢消息，单独作为一条slow消息记录
                         @JvmField var idleThreshold:Long = 100L,     // 消息间隔超过idleThreshold时, 消息间隔会被作为一条idle记录
                         @JvmField var recordMaxDuration:Int = 300,   // 单条记录最大时长, 连续多条消息时间总和在recordMaxDuration内的，会聚合记录在一条Msgs记录
                         @JvmField var timeLineDuration:Int = 35 * 1000,  // elapse只记录最近timeLineDuration时间内的记录，超过timeLineDuration的记录会被清除
                         @JvmField var logLevel: ElapseLogger.LogLevel = ElapseLogger.LogLevel.Error,
                         @JvmField var logger: ElapseLogger = ElapseLogger.Default
)
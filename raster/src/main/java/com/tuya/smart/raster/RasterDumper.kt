package com.tuya.smart.raster

import android.os.Process
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

/**
 * write down all slow message to file
 *
 * @author luqin.qin
 */
internal class RasterDumper: Thread("raster-dumper") {

    private val writeQueue: LinkedBlockingQueue<Any> = LinkedBlockingQueue()

    private var slowFileWriter: FileWriter

    private val dateFormat: SimpleDateFormat = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.CHINA)

    internal val dateTimeFormat: SimpleDateFormat = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.CHINA)


    private val logger = Raster.logger

    init {
        val fileName = "raster-slow-${dateFormat.format(Date())}-pid${Raster.myPid}.txt"
        val slowFile = File(Raster.context.getExternalFilesDir("raster"), fileName)
        slowFileWriter = FileWriter(slowFile)

        val fileHeader = """
Raster Slow Dump:
${Raster.applicationId}(${Process.myPid()}) tid=${Process.myPid()} uid=${Process.myUid()}
date: ${dateFormat.format(Date())}

        """.trimIndent()
        slowFileWriter.write(fileHeader)
        slowFileWriter.flush()
    }

    override fun run() {
        while (true) {
            val o = writeQueue.take()
            if (o is RasterRecord) {
                dumpSlowRecord(o, slowFileWriter)
            } else if (o is RasterTimeLine) {
                dumpTimeLine(o)
            }
        }
    }

    fun enqueue(o: Any) {
        writeQueue.offer(o)
    }

    private fun dumpTimeLine(timeLine: RasterTimeLine) {
        val fileName = "raster-dumper-${dateFormat.format(Date())}-pid${Raster.myPid}.txt"
        val dumperFile = File(Raster.context.getExternalFilesDir("raster"), fileName)
        val dumperWriter = FileWriter(dumperFile)
        val dumperHeader = """
Raster Dump:
${Raster.applicationId}(${Process.myPid()}) tid=${Process.myPid()} uid=${Process.myUid()}
date: ${dateFormat.format(Date())}
total count: ${timeLine.getCount()}

        """.trimIndent()
        dumperWriter.write(dumperHeader)
        timeLine.forEach {
            if (it.stackTrace != null) {
                dumpSlowRecord(it, dumperWriter)
            } else {
                val content = it.toString()
                if (Raster.logLevel >= RasterLogger.LogLevel.Debug) {
                    logger.d(msg = content)
                }
                dumperWriter.write(content)
                dumperWriter.write("\n")
                dumperWriter.flush()
            }
        }
    }

    private fun dumpSlowRecord(r: RasterRecord, writer: FileWriter) {
        r.apply {
            val formatRecord = """
------------ Slow Record (${dateTimeFormat.format(date)})  ------------
end:   timestamp=${end ?: "> ${Raster.enqueueDelay}"}  relative=${Util.relativeTime(end ?: Raster.enqueueDelay)}
start: timestamp=${start}  relative=${Util.relativeTime(start)} 
wallTime=${end?.minus(start)}   cpuTime=${cpuTime ?: "unknown"}
handler=${handler}
callback=${callback}
what=${what} ${if (handler.startsWith("Handler(android.app.ActivityThread\$H)")) {"(${RasterKeyMessages[what.toInt()]})"} else ""}
main thread stack:               
${stackTrace?.joinToString("\nat ", "at ")}


            """.trimIndent()
            if (Raster.logLevel >= RasterLogger.LogLevel.Slow) {
                logger.e(msg = formatRecord)
            }
            writer.write(formatRecord)
            writer.flush()
        }
    }


}
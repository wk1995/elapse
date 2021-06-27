package com.tuya.smart.raster

import android.annotation.SuppressLint
import android.os.Looper
import android.os.Message
import android.os.MessageQueue
import android.os.Process
import android.util.SparseArray
import androidx.core.util.set
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

    internal val dateTimeFormat: SimpleDateFormat = SimpleDateFormat(
        "MM-dd HH:mm:ss.SSS",
        Locale.CHINA
    )


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
        try {
            dumpPendingMessages(dumperWriter)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        dumperWriter.close()
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

    @SuppressLint("NewApi")
    private fun dumpPendingMessages(dumperWriter: FileWriter) {
        val queue = Looper.getMainLooper().queue // 21-23版本有这个api，只是方法被@hide标记，可以正常执行
        val mMessagesF = MessageQueue::class.java.getDeclaredField("mMessages")
        mMessagesF.isAccessible = true
        val mMessages = mMessagesF.get(queue) as Message?

        val nextF = Message::class.java.getDeclaredField("next")
        nextF.isAccessible = true

        dumperWriter.write("---------- Pending Messages ---------")

        var msg: Message? = mMessages
        while (msg != null) {
            dumpPendingMessage(msg, dumperWriter)
            msg = nextF.get(mMessages) as Message?
        }
        dumperWriter.flush()
    }

    private fun dumpPendingMessage(msg: Message, dumperWriter: FileWriter) {
        if (msg.target == null) {
            return
        }
        val pendingMessage: String
        when (msg.target.javaClass.name) {
            "android.app.ActivityThread\$H" -> {
                pendingMessage =
                    "Pending KeyMsg: ${
                        msg.toString().replaceFirst(
                            "what=${msg.what}",
                            "what=${RasterKeyMessages[msg.what]}(${msg.what})"
                        )
                    }"
            }
            "android.view.Choreographer\$FrameHandler" -> {
                val output = msg.toString().replaceFirst(
                    "what=${msg.what}",
                    "what=${frameKeys[msg.what]}(${msg.what})"
                )
                pendingMessage = if (msg.what == 2 && msg.arg1 == 0) { // input事件
                    ("Pending InputMsg: $output")
                } else {
                    ("Pending FrameMsg: $output")
                }
            }
            else -> {
                pendingMessage = ("Pending Msg: $msg")
            }
        }
        dumperWriter.write(pendingMessage)
        dumperWriter.write("\n")
    }


    companion object {
        private val frameKeys = SparseArray<String>()
        init {
            frameKeys[0] = "DO_FRAME"
            frameKeys[1] = "DO_SCHEDULE_VSYNC"
            frameKeys[2] = "DO_SCHEDULE_CALLBACK"
        }
    }

}
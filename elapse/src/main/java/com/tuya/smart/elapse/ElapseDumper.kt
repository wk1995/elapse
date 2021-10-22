package com.tuya.smart.elapse

import android.annotation.SuppressLint
import android.os.*
import android.util.SparseArray
import androidx.core.util.set
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

/**
 * write down all slow message to file
 *
 * @author luqin.qin
 */
internal class ElapseDumper: Thread("elapse-dumper") {

    private val writeQueue: LinkedBlockingQueue<Any> = LinkedBlockingQueue()

    private var slowFileWriter: FileWriter

    private val dateFormat: SimpleDateFormat = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.CHINA)

    internal val dateTimeFormat: SimpleDateFormat = SimpleDateFormat(
        "MM-dd HH:mm:ss.SSS",
        Locale.CHINA
    )


    private val logger = Elapse.logger

    init {
        val fileName = "elapse-slow-${dateFormat.format(Date())}-pid${Elapse.myPid}.txt"
        val slowFile = File(Elapse.context.getExternalFilesDir(Elapse.elapseDir), fileName)
        slowFileWriter = FileWriter(slowFile)

        val fileHeader = """
Elapse Slow Dump:
${Elapse.applicationId}(${Process.myPid()}) tid=${Process.myPid()} uid=${Process.myUid()}
date: ${dateFormat.format(Date())}

        """.trimIndent()
        slowFileWriter.write(fileHeader)
        slowFileWriter.flush()
    }

    override fun run() {
        while (true) {
            val o = writeQueue.take()
            if (o is ElapseRecord) {
                dumpSlowRecord(o, slowFileWriter)
            } else if (o is ElapseTimeLine) {
                dumpTimeLine(o)
            }
        }
    }

    fun enqueue(o: Any) {
        logger.i(msg = "enqueue message: $o")
        writeQueue.offer(o)
    }

    private fun dumpTimeLine(timeLine: ElapseTimeLine) {
        val fileName = "elapse-dumper-${dateFormat.format(Date())}-pid${Elapse.myPid}.txt"
        val dumperFile = File(Elapse.context.getExternalFilesDir(Elapse.elapseDir), fileName)
        val dumperWriter = FileWriter(dumperFile)
        val dumperHeader = """
Elapse Dump:
${Elapse.applicationId}(${Process.myPid()}) tid=${Process.myPid()} uid=${Process.myUid()}
date: ${dateFormat.format(Date())}
total count: ${timeLine.getCount()}

        """.trimIndent()
        dumperWriter.write(dumperHeader)
        timeLine.forEach {
            if (it.stackTrace != null) {
                dumpSlowRecord(it, dumperWriter)
            } else {
                val content = it.toString()
                if (Elapse.logLevel >= ElapseLogger.LogLevel.Debug) {
                    logger.d(msg = content)
                }
                dumperWriter.write(content)
                dumperWriter.write("\n")
                dumperWriter.flush()
            }
        }
        dumpRunningRecord(timeLine, dumperWriter)
        try {
            dumpPendingMessages(dumperWriter)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        logger.i(msg = "dumpsys meminfo ${Util.processName}")
        dumperWriter.write("\n==================  dumpsys memory info  ====================\n")
        executeShell(dumperWriter, "dumpsys meminfo ${Util.processName}", "dumpsys memory info failed!!")


//        dumperWriter.write("\n==================  threads info ${Elapse.myPid} ====================\n")
//        dumperWriter.flush()
//        executeShell(dumperWriter, "top -H -m 50 -n 1", "top -H failed!!")

        dumperWriter.close()
    }

    private fun dumpRunningRecord(timeLine: ElapseTimeLine, dumperWriter: FileWriter) {
        timeLine.runningRecord?.apply {
            val formatRecord = """
------------ Running Record (${dateTimeFormat.format(date)})  ------------
start: timestamp=${start}  relative=${Util.relativeTime(start)} 
end:   timestamp=${end}    relative=${Util.relativeTime(end ?: 0L)}
count = $count
wallTime${if (end != null) "=${end?.minus(start)}" else ">=${timeLine.anrTimestamp - start}"}   cpuTime=${cpuTime ?: "unknown"}
handler=${handler}
callback=${callback}
what=${what} ${if (handler.startsWith("Handler(android.app.ActivityThread\$H)")) {"(${ElapseKeyMessages[what.toInt()]})"} else ""}
main thread stack:               
${stackTrace?.joinToString("\nat ", "at ")}

            """.trimIndent()

            if (Elapse.logLevel >= ElapseLogger.LogLevel.Slow) {
                logger.e(msg = formatRecord)
            }
            dumperWriter.write(formatRecord)
            dumperWriter.flush()
        }
    }

    private fun executeShell(dumperWriter: FileWriter, command: String, err: String) {
        val runtime = Runtime.getRuntime()
        try {
            val process = runtime.exec(command)
            val streamReader = InputStreamReader(process.inputStream)
            val memInfo = streamReader.readText()
            dumperWriter.write(memInfo)
            dumperWriter.flush()
            logger.d(msg = memInfo)
            var errLines = 0
            val errReader = InputStreamReader(process.errorStream)
            errReader.readLines().onEach {
                if (errLines == 0) {
                    logger.e(msg = err)
                }
                logger.e(msg = it)
                errLines ++
            }
        } catch (e: Throwable){
            e.printStackTrace()
        }
    }

    private fun dumpSlowRecord(r: ElapseRecord, writer: FileWriter) {
        r.apply {
            val formatRecord = """
------------ Slow Record (${dateTimeFormat.format(date)})  ------------
start: timestamp=${start}  relative=${Util.relativeTime(start)} 
end:   timestamp=${end}    relative=${Util.relativeTime(end ?: 0L)}
wallTime${if (end != null) "=${end?.minus(start)}" else ">=${Elapse.enqueueDelay}"}   cpuTime=${cpuTime ?: "unknown"}
handler=${handler}
callback=${callback}
what=${what} ${if (handler.startsWith("Handler(android.app.ActivityThread\$H)")) {"(${ElapseKeyMessages[what.toInt()]})"} else ""}
main thread stack:               
${stackTrace?.joinToString("\nat ", "at ")}

            """.trimIndent()
            if (Elapse.logLevel >= ElapseLogger.LogLevel.Slow) {
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

        dumperWriter.write("---------- Pending Messages ---------\n")

        synchronized(queue) {
            var msg: Message? = mMessages
            var count = 0
            val curUptimeMills = SystemClock.uptimeMillis()
            while (msg != null) {
                count++
                dumpPendingMessage(curUptimeMills, msg, dumperWriter)
                msg = nextF.get(msg) as Message?
                if (count % 10 == 0) {
                    dumperWriter.flush()
                }
            }
            dumperWriter.write("total count: $count \n")
            dumperWriter.flush()
        }
    }

    private fun dumpPendingMessage(curUptimeMills: Long, msg: Message, dumperWriter: FileWriter) {
        if (msg.target == null) {
            return
        }
        val pendingMessage: String
        when (msg.target.javaClass.name) {
            "android.app.ActivityThread\$H" -> {
                pendingMessage =
                    "Pending KeyMsg: ${
                        messageToString(curUptimeMills, msg, keyMsg = true)
                    }"
            }
            "android.view.Choreographer\$FrameHandler" -> {
                val output = messageToString(curUptimeMills, msg, frameMsg = true)
                pendingMessage = if (msg.what == 2 && msg.arg1 == 0) { // input事件
                    ("Pending InputMsg: $output")
                } else {
                    ("Pending FrameMsg: $output")
                }
            }
            else -> {
                pendingMessage = ("Pending Msg: ${messageToString(curUptimeMills, msg)}")
            }
        }
        if (Elapse.logLevel >= ElapseLogger.LogLevel.Debug) {
            logger.d(msg = pendingMessage)
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

        private fun messageToString(curUptimeMills: Long, msg: Message, keyMsg: Boolean = false, frameMsg:Boolean = false): String {
            msg.apply {
                val b = StringBuilder()
                b.append("{ 0x${keyMsg.hashCode()} when=-")
                    .append(Util.formatTime(curUptimeMills - `when`))


                if (target != null) {
                    if (callback != null) {
                        b.append(" callback=")
                        b.append(callback.javaClass.name)
                    }
                    b.append(" what=")
                    when {
                        keyMsg -> {
                            b.append("${ElapseKeyMessages[what]}(${what})")
                        }
                        frameMsg -> {
                            b.append("${frameKeys[what]}(${what})")
                        }
                        else -> {
                            b.append(what)
                        }
                    }

                    if (arg1 != 0) {
                        b.append(" arg1=")
                        b.append(arg1)
                    }
                    if (arg2 != 0) {
                        b.append(" arg2=")
                        b.append(arg2)
                    }
                    if (obj != null) {
                        b.append(" obj=")
                        b.append(obj)
                    }
                    b.append(" target=")
                    b.append(target.javaClass.name)
                } else {
                    b.append(" barrier=")
                    b.append(arg1)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    if (isAsynchronous) {
                        b.append(" isAsynchronous=true")
                    }
                }
                b.append(" }")
                return b.toString()
            }
        }
    }

}
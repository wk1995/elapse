package com.tuya.smart.sample

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import com.tuya.smart.elapse.Elapse
import com.tuya.smart.elapse.ElapseLogger
import com.tuya.smart.elapse.ElapseOptions
import me.weishu.reflection.Reflection
import xcrash.ICrashCallback
import xcrash.XCrash

class APP: Application(), Handler.Callback {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        Reflection.unseal(base)

        val inits = XCrash.InitParameters()
        inits.enableAnrCrashHandler()
        inits.setAnrCallback { _, emergency ->
            Log.d("elapse", "anr found: $emergency")
            Elapse.dump()
        }
        XCrash.init(this, inits)

    }

    override fun onCreate() {
        super.onCreate()
        val options = ElapseOptions()
        options.slowThreshold = 300L
        options.logLevel = ElapseLogger.LogLevel.All
        Elapse.init(this, options)

        val h = Handler(Looper.getMainLooper(), this)
        Thread {
            while(true) {
                Thread.sleep(3)
                h.sendEmptyMessage(2000)
            }
        }
//            .start()
    }

    override fun handleMessage(msg: Message): Boolean {
        Thread.sleep(3)
        return true
    }
}
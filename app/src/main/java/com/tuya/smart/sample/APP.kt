package com.tuya.smart.sample

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import com.tuya.smart.elapse.Elapse
import com.tuya.smart.elapse.ElapseLogger
import com.tuya.smart.elapse.ElapseOptions
import me.weishu.reflection.Reflection

class APP: Application(), Handler.Callback {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        Reflection.unseal(base)
    }

    override fun onCreate() {
        super.onCreate()
        val options = ElapseOptions()
        options.logLevel = ElapseLogger.LogLevel.All
        options.enableFindSlowMethod = true
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